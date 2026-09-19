package com.newtech.note.service.impl;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.newtech.note.common.BusinessException;
import com.newtech.note.entity.dto.PointsChangeEnum;
import com.newtech.note.entity.dto.PointsConsumptionLease;
import com.newtech.note.entity.dto.UserPoints;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class PointsServiceMongoConcurrencyTest {
    private MongoServer server;
    private MongoClient client;
    private ReactiveMongoTemplate template;

    @BeforeEach
    void setUp() {
        server = new MongoServer(new MemoryBackend());
        server.bind("127.0.0.1", 0);
        client = MongoClients.create(server.getConnectionString());
        template = new ReactiveMongoTemplate(client, "points-concurrency");
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.shutdown();
    }

    @Test
    void twoServiceInstancesSharingMongoCannotBothOverspend() {
        template.insert(grant(42L, 10_000L)).block();
        PointsServiceImpl first = new PointsServiceImpl(template);
        PointsServiceImpl second = new PointsServiceImpl(template);

        Mono<Attempt> firstAttempt = consume(first).subscribeOn(Schedulers.parallel());
        Mono<Attempt> secondAttempt = consume(second).subscribeOn(Schedulers.parallel());

        StepVerifier.create(Mono.zip(firstAttempt, secondAttempt)
                        .flatMap(outcomes -> template.find(
                                        Query.query(Criteria.where("uid").is(42L)
                                                .and("changeMethod").is(PointsChangeEnum.CONSUMER)),
                                        UserPoints.class)
                                .count()
                                .map(count -> new Result(outcomes.getT1(), outcomes.getT2(), count))))
                .assertNext(result -> {
                    assertThat(result.successCount()).isEqualTo(1);
                    assertThat(result.failureCode()).isEqualTo("POINTS_INSUFFICIENT");
                    assertThat(result.persistedConsumptions()).isEqualTo(1L);
                })
                .verifyComplete();
    }

    @Test
    void lockOlderThanFormerLeaseIsNeverStolenAndSecondOwnerCannotCommit() {
        template.insert(grant(42L, 10_000L)).block();
        template.insert(consumption(42L, 1_000L, "first-owner-commit")).block();
        PointsConsumptionLease held = heldLock("active-owner",
                new Date(System.currentTimeMillis() - Duration.ofSeconds(61).toMillis()));
        template.insert(held).block();
        MongoPointsConsumptionLock contender = shortTimeoutLock();
        PointsServiceImpl secondOwner = new PointsServiceImpl(template, contender);

        StepVerifier.create(secondOwner.updatePoints(
                        42L, null, PointsChangeEnum.CONSUMER, null, 1_000L, null))
                .expectErrorSatisfies(this::assertLockUnavailable)
                .verify();

        assertThat(template.find(Query.query(Criteria.where("uid").is(42L)
                                .and("changeMethod").is(PointsChangeEnum.CONSUMER)),
                        UserPoints.class).count().block())
                .isEqualTo(1L);
        assertThat(template.findById(42L, PointsConsumptionLease.class).block())
                .extracting(PointsConsumptionLease::getOwner)
                .isEqualTo("active-owner");
    }

    @Test
    void oldOwnerCannotReleaseCurrentOwnersLock() {
        template.insert(heldLock("new-owner", new Date())).block();
        MongoPointsConsumptionLock lock = shortTimeoutLock();

        StepVerifier.create(lock.releaseOwned(42L, "old-owner"))
                .verifyComplete();

        assertThat(template.findById(42L, PointsConsumptionLease.class).block())
                .extracting(PointsConsumptionLease::getOwner)
                .isEqualTo("new-owner");
    }

    @Test
    void errorAndCancellationBothReleaseOwnedLock() {
        MongoPointsConsumptionLock lock = shortTimeoutLock();

        StepVerifier.create(lock.withLock(42L,
                        () -> Mono.error(new IllegalStateException("work failed"))))
                .expectErrorMessage("work failed")
                .verify();
        StepVerifier.create(lock.withLock(42L, () -> Mono.just("after-error")))
                .expectNext("after-error")
                .verifyComplete();

        AtomicBoolean cancelWorkStarted = new AtomicBoolean();
        StepVerifier.create(lock.withLock(42L, () -> {
                    cancelWorkStarted.set(true);
                    return Mono.never();
                }))
                .thenAwait(Duration.ofMillis(30))
                .thenCancel()
                .verify();
        assertThat(cancelWorkStarted).isTrue();

        StepVerifier.create(lock.withLock(42L, () -> Mono.just("after-cancel")))
                .expectNext("after-cancel")
                .verifyComplete();
    }

    @Test
    void orphanLockRejectsDeductionWithoutWritingLedger() {
        template.insert(grant(42L, 10_000L)).block();
        template.insert(heldLock("orphan-owner", new Date(0))).block();
        MongoPointsConsumptionLock lock = shortTimeoutLock();
        PointsServiceImpl service = new PointsServiceImpl(template, lock);

        StepVerifier.create(service.updatePoints(
                        42L, null, PointsChangeEnum.CONSUMER, null, 1_000L, null))
                .expectErrorSatisfies(this::assertLockUnavailable)
                .verify();

        assertThat(template.find(Query.query(Criteria.where("uid").is(42L)
                                .and("changeMethod").is(PointsChangeEnum.CONSUMER)),
                        UserPoints.class).count().block())
                .isZero();
    }

    private void assertLockUnavailable(Throwable error) {
        assertThat(error).isInstanceOfSatisfying(BusinessException.class,
                failure -> assertThat(failure.getCode()).isEqualTo("POINTS_LOCK_UNAVAILABLE"));
    }

    private MongoPointsConsumptionLock shortTimeoutLock() {
        return new MongoPointsConsumptionLock(
                template, Duration.ofMillis(80), Duration.ofMillis(10));
    }

    private PointsConsumptionLease heldLock(String owner, Date acquiredAt) {
        PointsConsumptionLease held = new PointsConsumptionLease();
        held.setUid(42L);
        held.setOwner(owner);
        held.setAcquiredAt(acquiredAt);
        return held;
    }

    private Mono<Attempt> consume(PointsServiceImpl service) {
        return service.updatePoints(42L, null, PointsChangeEnum.CONSUMER,
                        null, 6_000L, null)
                .map(ignored -> new Attempt(true, null))
                .onErrorResume(BusinessException.class,
                        failure -> Mono.just(new Attempt(false, failure.getCode())));
    }

    private UserPoints grant(long uid, long tokens) {
        UserPoints points = new UserPoints();
        points.setId("grant");
        points.setUid(uid);
        points.setToken(tokens);
        points.setChangePoint(tokens / 10_000.0);
        points.setChangeMethod(PointsChangeEnum.ADD);
        points.setCreatedAt(new Date());
        points.setExpireAt(new Date(System.currentTimeMillis() + 60_000));
        return points;
    }

    private UserPoints consumption(long uid, long tokens, String id) {
        UserPoints points = new UserPoints();
        points.setId(id);
        points.setUid(uid);
        points.setToken(tokens);
        points.setChangePoint(tokens / 10_000.0);
        points.setChangeMethod(PointsChangeEnum.CONSUMER);
        points.setCreatedAt(new Date());
        return points;
    }

    private record Attempt(boolean succeeded, String failureCode) {
    }

    private record Result(Attempt first, Attempt second, long persistedConsumptions) {
        long successCount() {
            return (first.succeeded() ? 1 : 0) + (second.succeeded() ? 1 : 0);
        }

        String failureCode() {
            return first.succeeded() ? second.failureCode() : first.failureCode();
        }
    }
}
