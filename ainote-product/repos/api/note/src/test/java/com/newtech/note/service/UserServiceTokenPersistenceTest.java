package com.newtech.note.service;

import com.mongodb.client.result.UpdateResult;
import com.newtech.note.entity.dto.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTokenPersistenceTest {
    private ReactiveMongoTemplate mongoTemplate;
    private UserService userService;

    @BeforeEach
    void setUp() {
        mongoTemplate = mock(ReactiveMongoTemplate.class);
        userService = new UserService(mongoTemplate, null, null, null, null);
    }

    @Test
    void compareAndSetUsesUidAndOldTokenAndRequiresOneModification() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(UserInfo.class)))
                .thenReturn(Mono.just(UpdateResult.acknowledged(1, 1L, null)));

        StepVerifier.create(userService.compareAndSetToken(42L, "old-token", "new-token"))
                .expectNext(true)
                .verifyComplete();

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).updateFirst(query.capture(), any(Update.class), eq(UserInfo.class));
        assertThat(query.getValue().getQueryObject())
                .containsEntry("uid", 42L)
                .containsEntry("token", "old-token");
    }

    @Test
    void compareAndSetRejectsZeroMatchZeroModificationAndUnacknowledgedWrites() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(UserInfo.class)))
                .thenReturn(
                        Mono.just(UpdateResult.acknowledged(0, 0L, null)),
                        Mono.just(UpdateResult.acknowledged(1, 0L, null)),
                        Mono.just(UpdateResult.unacknowledged()));

        StepVerifier.create(userService.compareAndSetToken(42L, "old", "new"))
                .expectNext(false)
                .verifyComplete();
        StepVerifier.create(userService.compareAndSetToken(42L, "old", "new"))
                .expectNext(false)
                .verifyComplete();
        StepVerifier.create(userService.compareAndSetToken(42L, "old", "new"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void loginPersistenceRequiresAnExistingMobileButAllowsAnIdenticalToken() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(UserInfo.class)))
                .thenReturn(
                        Mono.just(UpdateResult.acknowledged(0, 0L, null)),
                        Mono.just(UpdateResult.acknowledged(1, 0L, null)));

        StepVerifier.create(userService.updateTokenByMobile("13800000300", "token"))
                .expectNext(false)
                .verifyComplete();
        StepVerifier.create(userService.updateTokenByMobile("13800000300", "token"))
                .expectNext(true)
                .verifyComplete();
    }
}
