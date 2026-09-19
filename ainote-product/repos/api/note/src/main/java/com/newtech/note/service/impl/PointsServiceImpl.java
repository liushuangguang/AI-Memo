package com.newtech.note.service.impl;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.common.BusinessException;
import com.newtech.note.entity.dto.PointsChangeEnum;
import com.newtech.note.entity.dto.PointsDataEnum;
import com.newtech.note.entity.dto.UserPoints;
import com.newtech.note.service.PointsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class PointsServiceImpl implements PointsService {

    private final ReactiveMongoTemplate mongoTemplate;
    private final MongoPointsConsumptionLock consumptionLock;

    @Autowired
    public PointsServiceImpl(ReactiveMongoTemplate mongoTemplate) {
        this(mongoTemplate, new MongoPointsConsumptionLock(mongoTemplate));
    }

    PointsServiceImpl(ReactiveMongoTemplate mongoTemplate,
                      MongoPointsConsumptionLock consumptionLock) {
        this.mongoTemplate = mongoTemplate;
        this.consumptionLock = consumptionLock;
    }

    @Override
    public Mono<NoteBaseResponse<Void>> updatePoints(Long uid, String deviceId, PointsChangeEnum changeMethod, Double changePoint, Long token, PointsDataEnum days) {
        return Mono.defer(() -> {
            NormalizedUpdate update = normalizeUpdate(uid, changeMethod, changePoint, token, days);
            if (update.changeMethod() == PointsChangeEnum.CONSUMER) {
                return consumeWithDistributedLease(update, deviceId);
            }
            return insertUpdate(update, deviceId);
        });
    }

    private NormalizedUpdate normalizeUpdate(Long uid,
                                             PointsChangeEnum changeMethod,
                                             Double changePoint,
                                             Long token,
                                             PointsDataEnum days) {
        if (uid == null || uid <= 0) {
            throw invalidUpdate("uid must be a positive number");
        }
        if (changeMethod == null) {
            throw invalidUpdate("change method is required");
        }

        return switch (changeMethod) {
            case ADD -> {
                requirePositiveFinite(changePoint, "ADD changePoint");
                if (token != null || days == null) {
                    throw invalidUpdate("ADD requires changePoint and days only");
                }
                long normalizedToken = tokensForPoints(changePoint);
                yield new NormalizedUpdate(uid, changeMethod, exactPointsForToken(normalizedToken),
                        normalizedToken, days);
            }
            case CONSUMER -> {
                if (days != null || (changePoint == null) == (token == null)) {
                    throw invalidUpdate("CONSUMER requires exactly one of token or changePoint");
                }
                if (token != null) {
                    if (token <= 0) {
                        throw invalidUpdate("CONSUMER token must be positive");
                    }
                    double normalizedPoints = exactPointsForToken(token);
                    yield new NormalizedUpdate(uid, changeMethod, normalizedPoints, token, null);
                }
                requirePositiveFinite(changePoint, "CONSUMER changePoint");
                long normalizedToken = tokensForPoints(changePoint);
                yield new NormalizedUpdate(uid, changeMethod, exactPointsForToken(normalizedToken),
                        normalizedToken, null);
            }
            case EXPIRED -> throw invalidUpdate("EXPIRED records cannot be created through updatePoints");
        };
    }

    private long tokensForPoints(double changePoint) {
        try {
            long tokens = BigDecimal.valueOf(changePoint).movePointRight(4).longValueExact();
            if (tokens <= 0) {
                throw invalidUpdate("changePoint must represent at least one whole token");
            }
            return tokens;
        } catch (ArithmeticException failure) {
            throw invalidUpdate("changePoint must use 1/10000 precision and fit in a signed 64-bit token count");
        }
    }

    private double pointsForToken(long token) {
        return BigDecimal.valueOf(token).movePointLeft(4).doubleValue();
    }

    private double exactPointsForToken(long token) {
        double points = pointsForToken(token);
        try {
            if (tokensForPoints(points) != token) {
                throw invalidUpdate("token cannot be represented consistently by the Double points field");
            }
        } catch (IllegalArgumentException failure) {
            throw invalidUpdate("token cannot be represented consistently by the Double points field");
        }
        return points;
    }

    private void requirePositiveFinite(Double value, String field) {
        if (value == null || !Double.isFinite(value) || value <= 0.0) {
            throw invalidUpdate(field + " must be positive and finite");
        }
    }

    private IllegalArgumentException invalidUpdate(String message) {
        return new IllegalArgumentException("Invalid points update: " + message);
    }

    private Mono<NoteBaseResponse<Void>> consumeWithDistributedLease(NormalizedUpdate update,
                                                                      String deviceId) {
        return consumptionLock.withLock(update.uid(), () -> getAvailableTokens(update.uid(), null)
                .flatMap(availableTokens -> {
                    if (availableTokens < update.token()) {
                        return Mono.error(new BusinessException(
                                "POINTS_INSUFFICIENT", "Available points are insufficient"));
                    }
                    return insertUpdate(update, deviceId);
                }));
    }

    private Mono<NoteBaseResponse<Void>> insertUpdate(NormalizedUpdate update, String deviceId) {
        UserPoints userPoints = new UserPoints();
        Date now = new Date();
        if (update.changeMethod() == PointsChangeEnum.ADD) {
            Date expireAt = Date.from(LocalDateTime.now().plusDays(update.days().getDays())
                    .atZone(ZoneId.systemDefault()).toInstant());
            userPoints.setExpireAt(expireAt);
        }
        userPoints.setId(update.uid() + "_" + now.getTime() + "_" + UUID.randomUUID());
        userPoints.setUid(update.uid());
        userPoints.setDeviceId(deviceId);
        userPoints.setToken(update.token());
        userPoints.setChangeMethod(update.changeMethod());
        userPoints.setCreatedAt(now);
        userPoints.setChangePoint(update.changePoint());

        return mongoTemplate.insert(userPoints)
                .doOnNext(id -> log.info("update points success"))
                .doOnError(e -> log.error("update points error (failureType={})",
                        e.getClass().getSimpleName()))
                .thenReturn(NoteBaseResponse.success());
    }

    private record NormalizedUpdate(Long uid,
                                    PointsChangeEnum changeMethod,
                                    double changePoint,
                                    long token,
                                    PointsDataEnum days) {
    }

    @Override
    public Mono<Double> getAvailablePoints(Long uid, String deviceId) {
        return getAvailableTokens(uid, deviceId).map(this::pointsForToken);
    }

    private Mono<Long> getAvailableTokens(Long uid, String deviceId) {
        Date now = new Date();

        // 查询所有getChangeMethod等于ADD并且在过期时间之前的数据
        Query addQuery = new Query();
        if (Objects.nonNull(uid)) {
            addQuery.addCriteria(Criteria.where("uid").is(uid));
        }
        addQuery.addCriteria(Criteria.where("deviceId").is(deviceId));
        addQuery.addCriteria(Criteria.where("changeMethod").is(PointsChangeEnum.ADD));
        addQuery.addCriteria(Criteria.where("expireAt").gt(now));
        Flux<UserPoints> addFlux = mongoTemplate.find(addQuery, UserPoints.class);

        // 查询所有getChangeMethod等于CONSUMER但没有过期时间的数据
        Query consumerQuery = new Query();
        consumerQuery.addCriteria(Criteria.where("uid").is(uid));
        consumerQuery.addCriteria(Criteria.where("changeMethod").is(PointsChangeEnum.CONSUMER));
        consumerQuery.addCriteria(Criteria.where("expireAt").exists(false));
        Flux<UserPoints> consumerFlux = mongoTemplate.find(consumerQuery, UserPoints.class);

        // 合并两个Flux并计算结果
        return Mono.zip(
                        addFlux.map(this::recordTokens).reduce(0L, Math::addExact),
                        consumerFlux.map(this::recordTokens).reduce(0L, Math::addExact))
                .map(tuple -> Math.subtractExact(tuple.getT1(), tuple.getT2()))
                .map(total -> Math.max(total, 0L))
                .onErrorMap(ArithmeticException.class, failure -> new BusinessException(
                        "POINTS_LEDGER_INVALID", "Points ledger exceeds the supported range"));
    }

    private long recordTokens(UserPoints points) {
        if (points.getToken() != null) {
            if (points.getToken() < 0) {
                throw new BusinessException("POINTS_LEDGER_INVALID", "Points ledger contains an invalid record");
            }
            return points.getToken();
        }
        Double legacyPoints = points.getChangePoint();
        if (legacyPoints == null || !Double.isFinite(legacyPoints) || legacyPoints < 0) {
            throw new BusinessException("POINTS_LEDGER_INVALID", "Points ledger contains an invalid record");
        }
        try {
            return BigDecimal.valueOf(legacyPoints).movePointRight(4).longValueExact();
        } catch (ArithmeticException failure) {
            throw new BusinessException("POINTS_LEDGER_INVALID", "Points ledger contains an invalid record");
        }
    }

    @Override
    public Mono<List<UserPoints>> getPointsUpdateHistory(Long uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        return mongoTemplate.find(query, UserPoints.class)
                .collectList()
                .doOnNext(points -> log.info("get points history success"))
                .doOnError(e -> log.error("get points history error", e));
    }
}
