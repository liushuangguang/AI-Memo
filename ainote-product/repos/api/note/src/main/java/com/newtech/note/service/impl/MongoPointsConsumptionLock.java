package com.newtech.note.service.impl;

import com.newtech.note.common.BusinessException;
import com.newtech.note.entity.dto.PointsConsumptionLease;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Persistent Mongo exclusion lock. A uid can have exactly one lock document.
 * Locks never expire or get stolen: orphaned locks deliberately fail closed
 * until an operator verifies the prior write and removes the document.
 */
@Slf4j
final class MongoPointsConsumptionLock {
    private static final Duration DEFAULT_ACQUIRE_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration DEFAULT_RETRY_DELAY = Duration.ofMillis(25);

    private final ReactiveMongoTemplate mongoTemplate;
    private final Duration acquireTimeout;
    private final Duration retryDelay;

    MongoPointsConsumptionLock(ReactiveMongoTemplate mongoTemplate) {
        this(mongoTemplate, DEFAULT_ACQUIRE_TIMEOUT, DEFAULT_RETRY_DELAY);
    }

    MongoPointsConsumptionLock(ReactiveMongoTemplate mongoTemplate,
                               Duration acquireTimeout,
                               Duration retryDelay) {
        this.mongoTemplate = mongoTemplate;
        this.acquireTimeout = acquireTimeout;
        this.retryDelay = retryDelay;
    }

    <T> Mono<T> withLock(Long uid, Supplier<Mono<T>> work) {
        return Mono.usingWhen(
                acquire(uid),
                ignored -> Mono.defer(work),
                lease -> releaseOwned(lease.uid(), lease.owner()),
                (lease, failure) -> releaseOwned(lease.uid(), lease.owner()),
                lease -> releaseOwned(lease.uid(), lease.owner()));
    }

    private Mono<LockOwner> acquire(Long uid) {
        String owner = UUID.randomUUID().toString();
        long deadlineNanos = System.nanoTime() + acquireTimeout.toNanos();
        return attemptAcquire(uid, owner, deadlineNanos);
    }

    private Mono<LockOwner> attemptAcquire(Long uid, String owner, long deadlineNanos) {
        if (System.nanoTime() >= deadlineNanos) {
            return unavailable();
        }
        PointsConsumptionLease document = new PointsConsumptionLease();
        document.setUid(uid);
        document.setOwner(owner);
        document.setAcquiredAt(new Date());
        return mongoTemplate.insert(document)
                .map(ignored -> new LockOwner(uid, owner))
                .onErrorResume(DuplicateKeyException.class,
                        ignored -> retry(uid, owner, deadlineNanos));
    }

    private Mono<LockOwner> retry(Long uid, String owner, long deadlineNanos) {
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0) {
            return unavailable();
        }
        Duration delay = retryDelay.compareTo(Duration.ofNanos(remainingNanos)) < 0
                ? retryDelay
                : Duration.ofNanos(remainingNanos);
        return Mono.delay(delay).flatMap(ignored -> attemptAcquire(uid, owner, deadlineNanos));
    }

    Mono<Void> releaseOwned(Long uid, String owner) {
        Query ownedLock = Query.query(Criteria.where("_id").is(uid)
                .and("owner").is(owner));
        return mongoTemplate.remove(ownedLock, PointsConsumptionLease.class)
                .then()
                .onErrorResume(failure -> {
                    // Preserve fail-closed state: a failed release leaves the
                    // persistent lock for operational inspection.
                    log.warn("Unable to release points lock (failureType={})",
                            failure.getClass().getSimpleName());
                    return Mono.empty();
                });
    }

    private <T> Mono<T> unavailable() {
        return Mono.error(new BusinessException(
                "POINTS_LOCK_UNAVAILABLE", "Points balance is temporarily locked"));
    }

    private record LockOwner(Long uid, String owner) {
    }
}
