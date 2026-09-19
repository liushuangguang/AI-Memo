package com.newtech.note.controller;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.newtech.note.common.BusinessException;
import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.entity.dto.LoginRes;
import com.newtech.note.entity.dto.UserInfo;
import com.newtech.note.entity.dto.UserInfoDTO;
import com.newtech.note.entity.request.LoginReq;
import com.newtech.note.exception.NoteGlobalExceptionHandler;
import com.newtech.note.service.UserService;
import com.newtech.note.util.JWTUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerSecurityTest {
    private UserService userService;
    private JWTUtils jwtUtils;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        jwtUtils = mock(JWTUtils.class);
        controller = new AuthController(userService, jwtUtils);
    }

    @Test
    void newRegistrationUsesPositiveSnowflakeUserId() {
        LoginReq request = new LoginReq();
        request.setMobile("13800000100");
        request.setCode("654321");
        when(userService.checkExist(request.getMobile())).thenReturn(Mono.empty());
        when(userService.register(eq(request.getMobile()), eq(request.getCode()), anyLong()))
                .thenReturn(Mono.just(NoteBaseResponse.success(new LoginRes())));

        StepVerifier.create(controller.login(request))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<Long> uid = ArgumentCaptor.forClass(Long.class);
        verify(userService).register(eq(request.getMobile()), eq(request.getCode()), uid.capture());
        assertThat(uid.getValue()).isPositive();
    }

    @Test
    void malformedAuthorizationVariantsAreBusinessUnauthorized() {
        when(jwtUtils.checkJwt("not-a-jwt"))
                .thenThrow(new IllegalArgumentException("decoder details"));
        List<String> invalidHeaders = List.of(
                "", "Mob", "Bearer token", "Mobile", "Mobile ", "Mobilenot-a-jwt");

        invalidHeaders.forEach(header -> StepVerifier.create(controller.verifyToken(
                        MockServerHttpRequest.post("/auth/verifyToken")
                                .header(HttpHeaders.AUTHORIZATION, header)
                                .build()))
                .expectErrorSatisfies(this::assertUnauthorized)
                .verify());
    }

    @Test
    void malformedAuthorizationIsFixedHttp401WithRealControllerAdvice() {
        JWTUtils realJwtUtils = new JWTUtils();
        ReflectionTestUtils.setField(realJwtUtils, "jwtKey",
                "auth-controller-web-test-signing-key");
        ReflectionTestUtils.invokeMethod(realJwtUtils, "validateSigningKey");
        AuthController realController = new AuthController(userService, realJwtUtils);
        UserInfo user = new UserInfo();
        user.setUid(42L);
        user.setMobile("13800000102");
        String validToken = realJwtUtils.createJwt(user);
        int signatureStart = validToken.lastIndexOf('.') + 1;
        char replacement = validToken.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tampered = validToken.substring(0, signatureStart) + replacement
                + validToken.substring(signatureStart + 1);
        WebTestClient client = WebTestClient.bindToController(realController)
                .controllerAdvice(new NoteGlobalExceptionHandler())
                .build();

        assertHttpUnauthorized(client, null);
        assertHttpUnauthorized(client, "Bearer " + validToken);
        assertHttpUnauthorized(client, "Mobile ");
        assertHttpUnauthorized(client, "Mobile" + tampered);
    }

    @Test
    void refreshedTokenIsNotReturnedUntilCasAndAuthoritativeReadComplete() {
        String oldToken = "signed-token";
        String newToken = "new-signed-token";
        DecodedJWT decodedJWT = decodedJwt(42L, "13800000101");
        Sinks.One<Boolean> persisted = Sinks.one();
        when(jwtUtils.checkJwt(oldToken)).thenReturn(decodedJWT);
        when(jwtUtils.verifyToken(decodedJWT.getClaims())).thenReturn(-1);
        when(jwtUtils.createJwt(any(UserInfoDTO.class))).thenReturn(newToken);
        when(userService.compareAndSetToken(42L, oldToken, newToken))
                .thenReturn(persisted.asMono());
        when(userService.getTokenByUid(42L)).thenReturn(Mono.just(newToken));

        StepVerifier.create(controller.verifyToken(requestWithToken(oldToken)))
                .expectSubscription()
                .expectNoEvent(Duration.ofMillis(50))
                .then(() -> persisted.tryEmitValue(true))
                .assertNext(response -> assertThat(response.getData()).isEqualTo(newToken))
                .verifyComplete();
    }

    @Test
    void casZeroMatchAndDeletedAuthoritativeRecordCannotReturnSuccess() {
        String oldToken = "signed-token";
        String newToken = "new-signed-token";
        DecodedJWT decodedJWT = decodedJwt(42L, "13800000101");
        when(jwtUtils.checkJwt(oldToken)).thenReturn(decodedJWT);
        when(jwtUtils.verifyToken(decodedJWT.getClaims())).thenReturn(-1);
        when(jwtUtils.createJwt(any(UserInfoDTO.class))).thenReturn(newToken);
        when(userService.compareAndSetToken(42L, oldToken, newToken))
                .thenReturn(Mono.just(false), Mono.just(true));
        when(userService.getTokenByUid(42L)).thenReturn(Mono.empty());

        StepVerifier.create(controller.verifyToken(requestWithToken(oldToken)))
                .expectErrorSatisfies(this::assertUnauthorized)
                .verify();
        StepVerifier.create(controller.verifyToken(requestWithToken(oldToken)))
                .expectErrorSatisfies(this::assertUnauthorized)
                .verify();
    }

    @Test
    void concurrentRefreshCasLoserNeverReturnsTheOldToken() {
        String oldToken = "signed-token";
        String newToken = "new-signed-token";
        DecodedJWT decodedJWT = decodedJwt(42L, "13800000101");
        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger attempts = new AtomicInteger();
        when(jwtUtils.checkJwt(oldToken)).thenReturn(decodedJWT);
        when(jwtUtils.verifyToken(decodedJWT.getClaims())).thenReturn(-1);
        when(jwtUtils.createJwt(any(UserInfoDTO.class))).thenReturn(newToken);
        when(userService.compareAndSetToken(42L, oldToken, newToken))
                .thenAnswer(invocation -> Mono.fromCallable(() -> {
                    barrier.await();
                    return attempts.incrementAndGet() == 1;
                }));
        when(userService.getTokenByUid(42L)).thenReturn(Mono.just(newToken));

        List<Signal<NoteBaseResponse<String>>> results = Flux.merge(
                        controller.verifyToken(requestWithToken(oldToken))
                                .subscribeOn(Schedulers.parallel()).materialize(),
                        controller.verifyToken(requestWithToken(oldToken))
                                .subscribeOn(Schedulers.parallel()).materialize())
                .filter(signal -> signal.isOnNext() || signal.isOnError())
                .collectList()
                .block();

        assertThat(results).hasSize(2);
        assertThat(results.stream().filter(Signal::isOnNext).toList())
                .singleElement()
                .satisfies(signal -> assertThat(signal.get().getData()).isEqualTo(newToken));
        assertThat(results.stream().filter(Signal::isOnError).toList())
                .singleElement()
                .satisfies(signal -> assertUnauthorized(signal.getThrowable()));
    }

    @Test
    void stillValidTokenMustMatchDatabaseAuthority() {
        String token = "signed-token";
        DecodedJWT decodedJWT = decodedJwt(42L, "13800000101");
        when(jwtUtils.checkJwt(token)).thenReturn(decodedJWT);
        when(jwtUtils.verifyToken(decodedJWT.getClaims())).thenReturn(0);
        when(userService.getTokenByUid(42L)).thenReturn(Mono.just(token));

        StepVerifier.create(controller.verifyToken(requestWithToken(token)))
                .assertNext(response -> assertThat(response.getData()).isEqualTo(token))
                .verifyComplete();

        when(userService.getTokenByUid(42L)).thenReturn(Mono.just("newer-token"));
        StepVerifier.create(controller.verifyToken(requestWithToken(token)))
                .expectErrorSatisfies(this::assertUnauthorized)
                .verify();
    }

    private void assertHttpUnauthorized(WebTestClient client, String authorization) {
        WebTestClient.RequestHeadersSpec<?> request = client.post().uri("/auth/verifyToken");
        if (authorization != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        request.exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
                .expectBody(String.class).isEqualTo("Unauthorized");
    }

    private MockServerHttpRequest requestWithToken(String token) {
        return MockServerHttpRequest.post("/auth/verifyToken")
                .header(HttpHeaders.AUTHORIZATION, "Mobile" + token)
                .build();
    }

    private DecodedJWT decodedJwt(long uid, String mobile) {
        DecodedJWT decodedJWT = mock(DecodedJWT.class);
        Claim uidClaim = mock(Claim.class);
        Claim mobileClaim = mock(Claim.class);
        when(uidClaim.asLong()).thenReturn(uid);
        when(mobileClaim.asString()).thenReturn(mobile);
        when(decodedJWT.getClaim("uid")).thenReturn(uidClaim);
        when(decodedJWT.getClaim("mobile")).thenReturn(mobileClaim);
        when(decodedJWT.getClaims()).thenReturn(Map.of("uid", uidClaim, "mobile", mobileClaim));
        return decodedJWT;
    }

    private void assertUnauthorized(Throwable error) {
        assertThat(error).isInstanceOf(BusinessException.class);
        assertThat(((BusinessException) error).getCode()).isEqualTo("UNAUTHORIZED");
    }
}
