package com.newtech.note.security;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.newtech.note.common.BusinessException;
import com.newtech.note.service.UserService;
import com.newtech.note.util.JWTUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RequestIdentityServiceTest {
    private JWTUtils jwtUtils;
    private UserService userService;
    private RequestIdentityService service;

    @BeforeEach
    void setUp() {
        jwtUtils = mock(JWTUtils.class);
        userService = mock(UserService.class);
        service = new RequestIdentityService(jwtUtils, userService, true);
        when(jwtUtils.checkJwt(anyString())).thenAnswer(invocation -> jwtFor(
                "alice-token".equals(invocation.getArgument(0, String.class)) ? 1L : 2L));
        when(userService.getUidByToken("alice-token")).thenReturn(Mono.just(1L));
        when(userService.getUidByToken("bob-token")).thenReturn(Mono.just(2L));
    }

    @Test
    void exactGuestRequiresMatchingDeviceIdAndDoesNotTouchPersistence() {
        StepVerifier.create(service.resolve(guest("guest-a", "guest-a")))
                .assertNext(identity -> assertThat(identity.ownerId()).isEqualTo("guest-a"))
                .verifyComplete();

        verifyNoInteractions(jwtUtils, userService);
    }

    @Test
    void malformedBlankMismatchedAndUnicodeWhitespaceGuestsFailBeforePersistence() {
        List<MockServerHttpRequest> invalid = List.of(
                guest("", ""),
                guest("guest-a", "guest-b"),
                guest("guest\ta", "guest\ta"),
                guest("guest\u00a0a", "guest\u00a0a"),
                guest("guest\u3000a", "guest\u3000a"),
                guest("guest\u0007a", "guest\u0007a"));

        invalid.forEach(request -> StepVerifier.create(service.resolve(request))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(BusinessException.class))
                .verify());

        verifyNoInteractions(jwtUtils, userService);
    }

    @Test
    void positiveDecimalLongGuestIdsAreRejectedToPreventMobileOwnerCollision() {
        List<String> collidingIds = List.of("1", "0001", String.valueOf(Long.MAX_VALUE));

        collidingIds.forEach(id -> StepVerifier.create(service.resolve(guest(id, id)))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(BusinessException.class))
                .verify());

        StepVerifier.create(service.resolve(guest("0", "0")))
                .assertNext(identity -> assertThat(identity.ownerId()).isEqualTo("0"))
                .verifyComplete();
        StepVerifier.create(service.resolve(guest("9223372036854775808", "9223372036854775808")))
                .assertNext(identity -> assertThat(identity.guest()).isTrue())
                .verifyComplete();
        verifyNoInteractions(jwtUtils, userService);
    }

    @Test
    void sequentialRequestsOnOneThreadNeverReuseThePreviousUid() {
        StepVerifier.create(Flux.concat(
                        service.resolve(mobile("alice-token")),
                        service.resolve(mobile("bob-token")),
                        service.resolve(mobile("alice-token")))
                .map(RequestIdentityService.RequestIdentity::uid))
                .expectNext(1L, 2L, 1L)
                .verifyComplete();
    }

    @Test
    void concurrentRequestsOnReusedSchedulersRetainTheirOwnUid() {
        StepVerifier.create(Flux.range(0, 200)
                        .flatMap(index -> {
                            String token = index % 2 == 0 ? "alice-token" : "bob-token";
                            long expected = index % 2 == 0 ? 1L : 2L;
                            return service.resolve(mobile(token))
                                    .subscribeOn(Schedulers.parallel())
                                    .map(identity -> identity.uid() == expected);
                        }, 32)
                        .collectList())
                .assertNext(results -> assertThat(results).containsOnly(true))
                .verifyComplete();
    }

    @Test
    void authoritativeLookupMustMatchTheCurrentJwtClaim() {
        when(userService.getUidByToken("bob-token")).thenReturn(Mono.just(1L));

        StepVerifier.create(service.resolve(mobile("bob-token")))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) error).getCode()).isEqualTo("UNAUTHORIZED");
                })
                .verify();
    }

    @Test
    void mobileClaimAndAuthoritativeUidMustBothBePositive() {
        DecodedJWT zeroClaimJwt = jwtFor(0L);
        DecodedJWT negativeClaimJwt = jwtFor(-1L);
        when(jwtUtils.checkJwt("zero-claim-token")).thenReturn(zeroClaimJwt);
        when(jwtUtils.checkJwt("negative-claim-token")).thenReturn(negativeClaimJwt);
        when(userService.getUidByToken("alice-token")).thenReturn(Mono.just(0L));

        StepVerifier.create(service.resolve(mobile("zero-claim-token")))
                .expectError(BusinessException.class)
                .verify();
        StepVerifier.create(service.resolve(mobile("negative-claim-token")))
                .expectError(BusinessException.class)
                .verify();
        StepVerifier.create(service.resolve(mobile("alice-token")))
                .expectError(BusinessException.class)
                .verify();
    }

    private MockServerHttpRequest guest(String authorizationId, String deviceId) {
        return MockServerHttpRequest.get("/points/get")
                .header(HttpHeaders.AUTHORIZATION, "Guest " + authorizationId)
                .header("Device-Id", deviceId)
                .build();
    }

    private MockServerHttpRequest mobile(String token) {
        return MockServerHttpRequest.get("/points/get")
                .header(HttpHeaders.AUTHORIZATION, "Mobile" + token)
                .build();
    }

    private DecodedJWT jwtFor(long uid) {
        DecodedJWT jwt = mock(DecodedJWT.class);
        Claim claim = mock(Claim.class);
        when(claim.asLong()).thenReturn(uid);
        when(jwt.getClaim("uid")).thenReturn(claim);
        return jwt;
    }
}
