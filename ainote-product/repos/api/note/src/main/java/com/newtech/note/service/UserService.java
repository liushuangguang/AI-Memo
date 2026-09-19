package com.newtech.note.service;

import com.mongodb.client.result.UpdateResult;
import com.newtech.note.common.BusinessException;
import com.newtech.note.common.CodeTypeEnum;
import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.common.UserLevelEnum;
import com.newtech.note.common.constant.CaffeineCacheEnum;
import com.newtech.note.common.constant.RedisConstant;
import com.newtech.note.config.code.Sample;
import com.newtech.note.entity.dto.LoginRes;
import com.newtech.note.entity.dto.PointsChangeEnum;
import com.newtech.note.entity.dto.PointsDataEnum;
import com.newtech.note.entity.dto.UserInfo;
import com.newtech.note.util.IPUtil;
import com.newtech.note.util.JWTUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class UserService {

    private static final SecureRandom VERIFICATION_CODE_RANDOM = new SecureRandom();
    private static final int MAX_VERIFICATION_FAILURES = 5;
    private static final String PHONE_RATE_LIMIT_PREFIX = "phone:";
    private static final String PEER_RATE_LIMIT_PREFIX = "peer:";

    private final ReactiveMongoTemplate mongoTemplate;
    private final JWTUtils jwtUtils;
    private final PointsService pointsService;
    private final CacheManager cacheManager;
    private final Sample smsSender;

    public UserService(ReactiveMongoTemplate mongoTemplate,
                       JWTUtils jwtUtils,
                       PointsService pointsService,
                       CacheManager cacheManager,
                       Sample smsSender) {
        this.mongoTemplate = mongoTemplate;
        this.jwtUtils = jwtUtils;
        this.pointsService = pointsService;
        this.cacheManager = cacheManager;
        this.smsSender = smsSender;
    }

    public Mono<NoteBaseResponse<LoginRes>> register(String mobile, String code, Long uid) {
        if (uid == null || uid <= 0) {
            return Mono.error(new IllegalArgumentException("uid must be positive"));
        }
        return checkExist(mobile)
                .flatMap(user -> Mono.<NoteBaseResponse<LoginRes>>error(
                        new RuntimeException("Phone number already exists")))
                .switchIfEmpty(Mono.defer(() -> {
                    if (!consumeVerificationCode(mobile, code)) {
                        return Mono.error(new RuntimeException("验证码不正确"));
                    }

                    UserInfo user = new UserInfo();
                    user.setMobile(mobile);
                    user.setUid(uid);
                    user.setNickName("用户" + user.getUid());
                    user.setCreatedAt(new Date());
                    user.setUpdateAt(new Date());
                    user.setLevel(UserLevelEnum.HUMAN);
                    String jwt = jwtUtils.createJwt(user);
                    user.setToken(jwt);
                    return mongoTemplate.insert(user)
                            .flatMap(inserted -> {
                                LoginRes loginRes = new LoginRes();
                                loginRes.setToken(jwt);
                                pointsService.updatePoints(uid, null, PointsChangeEnum.ADD,
                                                UserLevelEnum.HUMAN.getPoint(), null,
                                                PointsDataEnum.MONTH)
                                        .subscribe();
                                return Mono.just(NoteBaseResponse.success(loginRes));
                            });
                }));
    }

    public Mono<NoteBaseResponse<LoginRes>> login(String mobile, String code) {
        return checkExist(mobile)
                .switchIfEmpty(Mono.error(new RuntimeException("Phone number not exists")))
                .flatMap(user -> {
                    if (!consumeVerificationCode(mobile, code)) {
                        return Mono.error(new RuntimeException("验证码不正确"));
                    }
                    String jwt = jwtUtils.createJwt(user);
                    LoginRes loginRes = new LoginRes();
                    loginRes.setToken(jwt);
                    return updateTokenByMobile(mobile, jwt)
                            .flatMap(updated -> updated
                                    ? Mono.just(NoteBaseResponse.success(loginRes))
                                    : Mono.error(unauthorized()));
                });
    }

    /**
     * Sends and stores a challenge only after Aliyun explicitly confirms success.
     * The phone-and-IP reservation is shared by this application's Spring cache manager.
     */
    public Mono<NoteBaseResponse<Void>> sendSMSCode(
            String phone, CodeTypeEnum type, ServerHttpRequest request) {
        if (StringUtils.isBlank(phone) || type == null || request == null) {
            return Mono.error(new RuntimeException("手机号或验证码类型为空"));
        }

        return Mono.fromCallable(() -> {
            com.github.benmanes.caffeine.cache.Cache<Object, Object> challengeCache =
                    nativeCache(CaffeineCacheEnum.MOBILE_SMSCODE);
            com.github.benmanes.caffeine.cache.Cache<Object, Object> rateLimitCache =
                    nativeCache(CaffeineCacheEnum.MOBILE_SMS_RATE_LIMIT);
            Object reservation = new Object();
            String phoneRateLimitKey = PHONE_RATE_LIMIT_PREFIX + phone;
            String peerRateLimitKey = PEER_RATE_LIMIT_PREFIX + IPUtil.getRequestIp(request);
            if (rateLimitCache.asMap().putIfAbsent(phoneRateLimitKey, reservation) != null) {
                log.warn("SMS send rate limit exceeded");
                throw new RuntimeException("短信发送频率太快，请稍后再尝试");
            }
            if (rateLimitCache.asMap().putIfAbsent(peerRateLimitKey, reservation) != null) {
                rateLimitCache.asMap().remove(phoneRateLimitKey, reservation);
                log.warn("SMS send rate limit exceeded");
                throw new RuntimeException("短信发送频率太快，请稍后再尝试");
            }

            String verificationCode = String.valueOf(
                    1_000_000 + VERIFICATION_CODE_RANDOM.nextInt(1_000_000)).substring(1);
            boolean sent;
            try {
                sent = smsSender.sendingCode(phone, verificationCode, type);
            } catch (RuntimeException failure) {
                releaseRateLimitReservation(
                        rateLimitCache, phoneRateLimitKey, peerRateLimitKey, reservation);
                throw new RuntimeException("验证码发送失败");
            }
            if (!sent) {
                releaseRateLimitReservation(
                        rateLimitCache, phoneRateLimitKey, peerRateLimitKey, reservation);
                throw new RuntimeException("验证码发送失败");
            }

            challengeCache.put(verificationCodeKey(phone), new SmsChallenge(verificationCode, 0));
            return NoteBaseResponse.<Void>success();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private void releaseRateLimitReservation(
            com.github.benmanes.caffeine.cache.Cache<Object, Object> rateLimitCache,
            String phoneRateLimitKey,
            String peerRateLimitKey,
            Object reservation) {
        rateLimitCache.asMap().remove(phoneRateLimitKey, reservation);
        rateLimitCache.asMap().remove(peerRateLimitKey, reservation);
    }

    private boolean consumeVerificationCode(String mobile, String submittedCode) {
        if (StringUtils.isBlank(mobile) || StringUtils.isBlank(submittedCode)) {
            return false;
        }
        AtomicBoolean consumed = new AtomicBoolean(false);
        nativeCache(CaffeineCacheEnum.MOBILE_SMSCODE).asMap().compute(
                verificationCodeKey(mobile), (key, storedValue) -> {
                    SmsChallenge challenge = storedValue instanceof SmsChallenge smsChallenge
                            ? smsChallenge : null;
                    if (challenge == null) {
                        return null;
                    }
                    if (MessageDigest.isEqual(
                            challenge.code().getBytes(StandardCharsets.US_ASCII),
                            submittedCode.getBytes(StandardCharsets.US_ASCII))) {
                        consumed.set(true);
                        return null;
                    }
                    int failedAttempts = challenge.failedAttempts() + 1;
                    if (failedAttempts >= MAX_VERIFICATION_FAILURES) {
                        return null;
                    }
                    return new SmsChallenge(challenge.code(), failedAttempts);
                });
        return consumed.get();
    }

    static record SmsChallenge(String code, int failedAttempts) {
        SmsChallenge {
            if (StringUtils.isBlank(code) || failedAttempts < 0
                    || failedAttempts >= MAX_VERIFICATION_FAILURES) {
                throw new IllegalArgumentException("Invalid SMS challenge state");
            }
        }
    }

    private String verificationCodeKey(String mobile) {
        return RedisConstant.MOBILE_SMSCODE + mobile;
    }

    /**
     * These atomic operations are process-local. Multi-instance deployments must use a shared
     * atomic store such as Redis for challenges and rate-limit reservations.
     */
    private com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache(
            CaffeineCacheEnum cacheName) {
        Cache cache = cacheManager.getCache(cacheName.name());
        if (!(cache instanceof CaffeineCache caffeineCache)) {
            throw new IllegalStateException("SMS cache is unavailable");
        }
        return caffeineCache.getNativeCache();
    }

    public Mono<UserInfo> checkExist(String mobile) {
        Query query = new Query();
        query.addCriteria(Criteria.where("mobile").is(mobile));
        return mongoTemplate.findOne(query, UserInfo.class);
    }

    public Mono<UserInfo> getUserLevel(Long uid) {
        Query query = new Query();
        query.addCriteria(Criteria.where("uid").is(uid));
        query.fields().include("level");
        return mongoTemplate.findOne(query, UserInfo.class);
    }

    public Mono<Long> getUidByToken(String token) {
        Query query = new Query(Criteria.where("token").is(token));
        return mongoTemplate.findOne(query, UserInfo.class)
                .map(UserInfo::getUid);
    }

    public Mono<Boolean> updateTokenByMobile(String mobile, String token) {
        Query query = new Query(Criteria.where("mobile").is(mobile));
        Update update = new Update().set("token", token);
        return mongoTemplate.updateFirst(query, update, UserInfo.class)
                .map(result -> result.wasAcknowledged() && result.getMatchedCount() == 1);
    }

    public Mono<Void> updateTokenByUid(String uid, String token) {
        Query query = new Query(Criteria.where("uid").is(uid));
        Update update = new Update().set("token", token);
        return mongoTemplate.updateFirst(query, update, UserInfo.class).then();
    }

    public Mono<Boolean> compareAndSetToken(Long uid, String oldToken, String newToken) {
        if (uid == null || uid <= 0 || StringUtils.isAnyBlank(oldToken, newToken)) {
            return Mono.just(false);
        }
        Query query = new Query(Criteria.where("uid").is(uid).and("token").is(oldToken));
        Update update = new Update().set("token", newToken);
        return mongoTemplate.updateFirst(query, update, UserInfo.class)
                .map(this::isSingleModifiedRecord);
    }

    public Mono<String> getTokenByUid(Long uid) {
        if (uid == null || uid <= 0) {
            return Mono.empty();
        }
        Query query = new Query(Criteria.where("uid").is(uid));
        query.fields().include("token");
        return mongoTemplate.findOne(query, UserInfo.class)
                .map(UserInfo::getToken)
                .filter(StringUtils::isNotBlank);
    }

    private boolean isSingleModifiedRecord(UpdateResult result) {
        return result.wasAcknowledged()
                && result.getMatchedCount() == 1
                && result.getModifiedCount() == 1;
    }

    private BusinessException unauthorized() {
        return new BusinessException("UNAUTHORIZED", "The authenticated user no longer exists");
    }
}
