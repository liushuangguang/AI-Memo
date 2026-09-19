package com.newtech.note.service.impl;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.entity.dto.PointsChangeEnum;
import com.newtech.note.entity.dto.PointsDataEnum;
import com.newtech.note.entity.dto.UserPoints;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PointsServiceImplTest {

    @ParameterizedTest(name = "rejects invalid update: {0}")
    @MethodSource("invalidUpdates")
    void rejectsInvalidInternalUpdates(String description,
                                       Long uid,
                                       PointsChangeEnum method,
                                       Double points,
                                       Long tokens,
                                       PointsDataEnum days) {
        ReactiveMongoTemplate mongoTemplate = mock(ReactiveMongoTemplate.class);
        PointsServiceImpl service = new PointsServiceImpl(mongoTemplate);

        StepVerifier.create(service.updatePoints(uid, null, method, points, tokens, days))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(mongoTemplate, never()).insert(any(UserPoints.class));
    }

    private static Stream<Arguments> invalidUpdates() {
        return Stream.of(
                Arguments.of("null uid", null, PointsChangeEnum.ADD, 1.0, null, PointsDataEnum.MONTH),
                Arguments.of("zero uid", 0L, PointsChangeEnum.ADD, 1.0, null, PointsDataEnum.MONTH),
                Arguments.of("missing method", 1L, null, 1.0, null, PointsDataEnum.MONTH),
                Arguments.of("negative add", 1L, PointsChangeEnum.ADD, -1.0, null, PointsDataEnum.MONTH),
                Arguments.of("sub-token add", 1L, PointsChangeEnum.ADD, 0.00009, null, PointsDataEnum.MONTH),
                Arguments.of("fractional token add", 1L, PointsChangeEnum.ADD, 0.00019, null, PointsDataEnum.MONTH),
                Arguments.of("overflowing add", 1L, PointsChangeEnum.ADD, Double.MAX_VALUE, null, PointsDataEnum.MONTH),
                Arguments.of("NaN add", 1L, PointsChangeEnum.ADD, Double.NaN, null, PointsDataEnum.MONTH),
                Arguments.of("infinite add", 1L, PointsChangeEnum.ADD, Double.POSITIVE_INFINITY, null, PointsDataEnum.MONTH),
                Arguments.of("missing add days", 1L, PointsChangeEnum.ADD, 1.0, null, null),
                Arguments.of("add with token", 1L, PointsChangeEnum.ADD, 1.0, 1L, PointsDataEnum.MONTH),
                Arguments.of("zero consumer token", 1L, PointsChangeEnum.CONSUMER, null, 0L, null),
                Arguments.of("negative consumer token", 1L, PointsChangeEnum.CONSUMER, null, -1L, null),
                Arguments.of("non-roundtrippable consumer token", 1L, PointsChangeEnum.CONSUMER,
                        null, 9_007_199_254_740_993L, null),
                Arguments.of("max consumer token", 1L, PointsChangeEnum.CONSUMER,
                        null, Long.MAX_VALUE, null),
                Arguments.of("NaN consumer points", 1L, PointsChangeEnum.CONSUMER, Double.NaN, null, null),
                Arguments.of("infinite consumer points", 1L, PointsChangeEnum.CONSUMER, Double.NEGATIVE_INFINITY, null, null),
                Arguments.of("both consumer units", 1L, PointsChangeEnum.CONSUMER, 1.0, 1L, null),
                Arguments.of("missing consumer units", 1L, PointsChangeEnum.CONSUMER, null, null, null),
                Arguments.of("consumer with days", 1L, PointsChangeEnum.CONSUMER, null, 1L, PointsDataEnum.MONTH),
                Arguments.of("expired method", 1L, PointsChangeEnum.EXPIRED, 1.0, null, null));
    }

    @Test
    void validInternalAddIsPersisted() {
        ReactiveMongoTemplate mongoTemplate = mock(ReactiveMongoTemplate.class);
        AtomicReference<UserPoints> inserted = new AtomicReference<>();
        when(mongoTemplate.insert(any(UserPoints.class))).thenAnswer(invocation -> {
            UserPoints value = invocation.getArgument(0);
            inserted.set(value);
            return Mono.just(value);
        });
        PointsServiceImpl service = new PointsServiceImpl(mongoTemplate);

        StepVerifier.create(service.updatePoints(
                        42L, null, PointsChangeEnum.ADD, 10.0, null, PointsDataEnum.MONTH))
                .expectNextMatches(NoteBaseResponse::isSuccess)
                .verifyComplete();

        assertThat(inserted.get().getUid()).isEqualTo(42L);
        assertThat(inserted.get().getChangePoint()).isEqualTo(10.0);
        assertThat(inserted.get().getToken()).isEqualTo(100_000L);
        assertThat(inserted.get().getExpireAt()).isAfter(new Date());
    }

    @Test
    void minimumPrecisionIsStoredFromTheNormalizedWholeToken() {
        ReactiveMongoTemplate mongoTemplate = mock(ReactiveMongoTemplate.class);
        AtomicReference<UserPoints> inserted = new AtomicReference<>();
        when(mongoTemplate.insert(any(UserPoints.class))).thenAnswer(invocation -> {
            UserPoints value = invocation.getArgument(0);
            inserted.set(value);
            return Mono.just(value);
        });
        PointsServiceImpl service = new PointsServiceImpl(mongoTemplate);

        StepVerifier.create(service.updatePoints(
                        42L, null, PointsChangeEnum.ADD, 0.0001, null, PointsDataEnum.MONTH))
                .expectNextMatches(NoteBaseResponse::isSuccess)
                .verifyComplete();

        assertThat(inserted.get().getToken()).isEqualTo(1L);
        assertThat(inserted.get().getChangePoint()).isEqualTo(0.0001);
    }
}
