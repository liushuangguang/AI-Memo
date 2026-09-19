package com.newtech.note.service;

import com.newtech.note.common.CodeTypeEnum;
import com.newtech.note.common.BusinessException;
import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.common.constant.CaffeineCacheEnum;
import com.newtech.note.common.constant.RedisConstant;
import com.newtech.note.config.CaffeineCacheConfig;
import com.newtech.note.config.code.Sample;
import com.newtech.note.entity.dto.LoginRes;
import com.newtech.note.entity.dto.UserInfo;
import com.newtech.note.util.JWTUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.List;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceSmsSecurityTest {
    private ReactiveMongoTemplate mongoTemplate;
    private JWTUtils jwtUtils;
    private PointsService pointsService;
    private Sample smsSender;
    private CacheManager cacheManager;
    private UserService service;

    @BeforeEach
    void setUp() {
        mongoTemplate = mock(ReactiveMongoTemplate.class);
        jwtUtils = mock(JWTUtils.class);
        pointsService = mock(PointsService.class);
        smsSender = mock(Sample.class);
        cacheManager = new CaffeineCacheConfig().caffeineCacheManager();
        ((SimpleCacheManager) cacheManager).afterPropertiesSet();
        service = spy(new UserService(
                mongoTemplate, jwtUtils, pointsService, cacheManager, smsSender));
    }

    @Test
    void generatedCodeIsSixDigitsAndKnown1234CannotAuthenticateOrConsumeIt() {
        when(smsSender.sendingCode(anyString(), anyString(), eq(CodeTypeEnum.LOGIN)))
                .thenReturn(true);
        String phone = "13800000000";
        service.sendSMSCode(phone, CodeTypeEnum.LOGIN, request("10.0.0.1")).block();
        String generatedCode = cachedCode(phone);
        assertThat(generatedCode).matches("\\d{6}").isNotEqualTo("1234");

        stubExistingUser(phone);
        StepVerifier.create(service.login(phone, "1234"))
                .expectErrorMessage("验证码不正确")
                .verify();
        assertThat(cachedCode(phone)).isEqualTo(generatedCode);

        StepVerifier.create(service.login(phone, generatedCode))
                .assertNext(response -> assertThat(response.isSuccess()).isTrue())
                .verifyComplete();
        StepVerifier.create(service.login(phone, generatedCode))
                .expectErrorMessage("验证码不正确")
                .verify();
    }

    @Test
    void fifthWrongAttemptDestroysChallengeButEarlierAttemptsDoNot() {
        String phone = "13800000021";
        putCode(phone, "654321");
        stubExistingUser(phone);

        for (int attempt = 1; attempt < 5; attempt++) {
            StepVerifier.create(service.login(phone, "000000"))
                    .expectErrorMessage("验证码不正确")
                    .verify();
            assertThat(cachedChallenge(phone).failedAttempts()).isEqualTo(attempt);
        }

        StepVerifier.create(service.login(phone, "000000"))
                .expectErrorMessage("验证码不正确")
                .verify();
        assertThat(cachedChallenge(phone)).isNull();
        StepVerifier.create(service.login(phone, "654321"))
                .expectErrorMessage("验证码不正确")
                .verify();
    }

    @Test
    void correctCodeStillSucceedsAfterFourWrongAttempts() {
        String phone = "13800000022";
        putCode(phone, "654321");
        stubExistingUser(phone);

        for (int attempt = 0; attempt < 4; attempt++) {
            StepVerifier.create(service.login(phone, "000000"))
                    .expectErrorMessage("验证码不正确")
                    .verify();
        }
        StepVerifier.create(service.login(phone, "654321"))
                .assertNext(response -> assertThat(response.isSuccess()).isTrue())
                .verifyComplete();
    }

    @Test
    void concurrentLoginConsumptionAllowsExactlyOneSuccess() {
        String phone = "13800000001";
        putCode(phone, "654321");
        stubExistingUser(phone);

        List<Boolean> results = Flux.merge(
                        loginResult(phone, "654321"),
                        loginResult(phone, "654321"))
                .collectList()
                .block();

        assertThat(results).containsExactlyInAnyOrder(true, false);
        verify(service, times(1)).updateTokenByMobile(phone, "jwt");
    }

    @Test
    void loginWaitsForMatchedTokenPersistenceBeforeResponding() {
        String phone = "13800000026";
        putCode(phone, "654321");
        UserInfo user = stubExistingUserWithoutTokenUpdate(phone);
        Sinks.One<Boolean> persisted = Sinks.one();
        when(jwtUtils.createJwt(user)).thenReturn("jwt");
        doReturn(persisted.asMono()).when(service).updateTokenByMobile(phone, "jwt");

        StepVerifier.create(service.login(phone, "654321"))
                .expectSubscription()
                .expectNoEvent(Duration.ofMillis(50))
                .then(() -> persisted.tryEmitValue(true))
                .assertNext(response -> assertThat(response.getData().getToken()).isEqualTo("jwt"))
                .verifyComplete();
    }

    @Test
    void loginZeroMatchedTokenUpdateCannotReturnSuccess() {
        String phone = "13800000027";
        putCode(phone, "654321");
        UserInfo user = stubExistingUserWithoutTokenUpdate(phone);
        when(jwtUtils.createJwt(user)).thenReturn("jwt");
        doReturn(Mono.just(false)).when(service).updateTokenByMobile(phone, "jwt");

        StepVerifier.create(service.login(phone, "654321"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) error).getCode()).isEqualTo("UNAUTHORIZED");
                })
                .verify();
    }

    @Test
    void registrationConsumesCodeOnce() {
        String phone = "13800000002";
        putCode(phone, "654321");
        doReturn(Mono.empty()).when(service).checkExist(phone);
        when(jwtUtils.createJwt(any(UserInfo.class))).thenReturn("jwt");
        when(mongoTemplate.insert(any(UserInfo.class))).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0)));
        when(pointsService.updatePoints(any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(NoteBaseResponse.success()));

        StepVerifier.create(service.register(phone, "654321", 10L))
                .assertNext(response -> assertThat(response.isSuccess()).isTrue())
                .verifyComplete();
        StepVerifier.create(service.register(phone, "654321", 11L))
                .expectErrorMessage("验证码不正确")
                .verify();
        verify(mongoTemplate, times(1)).insert(any(UserInfo.class));
    }

    @Test
    void sharedSpringCacheRateLimitsPhoneAndSocketPeerSeparatelyForSixtySeconds() {
        when(smsSender.sendingCode(anyString(), anyString(), any())).thenReturn(true);
        UserService secondInstance = new UserService(
                mongoTemplate, jwtUtils, pointsService, cacheManager, smsSender);
        String phone = "13800000003";
        ServerHttpRequest request = request("10.0.0.3");

        StepVerifier.create(service.sendSMSCode(phone, CodeTypeEnum.LOGIN, request))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(secondInstance.sendSMSCode(phone, CodeTypeEnum.LOGIN, request))
                .expectErrorMessage("短信发送频率太快，请稍后再尝试")
                .verify();
        StepVerifier.create(secondInstance.sendSMSCode(
                        "13800000999", CodeTypeEnum.LOGIN, request("10.0.0.3")))
                .expectErrorMessage("短信发送频率太快，请稍后再尝试")
                .verify();
        StepVerifier.create(secondInstance.sendSMSCode(
                        phone, CodeTypeEnum.LOGIN, request("10.0.0.99")))
                .expectErrorMessage("短信发送频率太快，请稍后再尝试")
                .verify();

        assertThat(CaffeineCacheEnum.MOBILE_SMS_RATE_LIMIT.getDuration()).isEqualTo(60);
        assertThat(CaffeineCacheEnum.MOBILE_SMS_RATE_LIMIT.getUnit()).isEqualTo(TimeUnit.SECONDS);
        verify(smsSender, times(1)).sendingCode(anyString(), anyString(), any());
    }

    @Test
    void forwardedForCannotBypassSocketPeerLimit() {
        when(smsSender.sendingCode(anyString(), anyString(), any())).thenReturn(true);

        StepVerifier.create(service.sendSMSCode(
                        "13800000023", CodeTypeEnum.LOGIN,
                        request("10.0.0.23", "198.51.100.1")))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(service.sendSMSCode(
                        "13800000024", CodeTypeEnum.LOGIN,
                        request("10.0.0.23", "198.51.100.2")))
                .expectErrorMessage("短信发送频率太快，请稍后再尝试")
                .verify();
    }

    @Test
    void newSuccessfulSendReplacesThePreviousChallenge() {
        when(smsSender.sendingCode(anyString(), anyString(), any())).thenReturn(true);
        String phone = "13800000025";
        service.sendSMSCode(phone, CodeTypeEnum.LOGIN, request("10.0.0.25")).block();
        String firstCode = cachedCode(phone);

        nativeCache(CaffeineCacheEnum.MOBILE_SMS_RATE_LIMIT).invalidateAll();
        service.sendSMSCode(phone, CodeTypeEnum.LOGIN, request("10.0.0.25")).block();
        String replacementCode = cachedCode(phone);
        while (replacementCode.equals(firstCode)) {
            nativeCache(CaffeineCacheEnum.MOBILE_SMS_RATE_LIMIT).invalidateAll();
            service.sendSMSCode(phone, CodeTypeEnum.LOGIN, request("10.0.0.25")).block();
            replacementCode = cachedCode(phone);
        }

        stubExistingUser(phone);
        StepVerifier.create(service.login(phone, firstCode))
                .expectErrorMessage("验证码不正确")
                .verify();
        StepVerifier.create(service.login(phone, replacementCode))
                .assertNext(response -> assertThat(response.isSuccess()).isTrue())
                .verifyComplete();
    }

    @Test
    void providerFailureDoesNotCacheChallengeAndReleasesRateReservation() {
        String phone = "13800000004";
        ServerHttpRequest request = request("10.0.0.4");
        when(smsSender.sendingCode(anyString(), anyString(), any()))
                .thenReturn(false, true);

        StepVerifier.create(service.sendSMSCode(phone, CodeTypeEnum.LOGIN, request))
                .expectErrorMessage("验证码发送失败")
                .verify();
        assertThat(cachedCode(phone)).isNull();

        StepVerifier.create(service.sendSMSCode(phone, CodeTypeEnum.LOGIN, request))
                .expectNextCount(1)
                .verifyComplete();
        assertThat(cachedCode(phone)).matches("\\d{6}");
    }

    @Test
    void cacheRetainsMoreThanSevenSimultaneousPhoneChallenges() {
        when(smsSender.sendingCode(anyString(), anyString(), any())).thenReturn(true);

        for (int index = 0; index < 8; index++) {
            String phone = "1380000001" + index;
            service.sendSMSCode(phone, CodeTypeEnum.LOGIN, request("10.0.1." + index)).block();
            assertThat(cachedCode(phone)).matches("\\d{6}");
        }

        assertThat(CaffeineCacheEnum.MOBILE_SMSCODE.getMaxSize()).isGreaterThan(1_000);
        assertThat(nativeCache(CaffeineCacheEnum.MOBILE_SMSCODE).estimatedSize()).isEqualTo(8);
    }

    @Test
    void providerExceptionDoesNotCacheChallenge() {
        String phone = "13800000020";
        when(smsSender.sendingCode(anyString(), anyString(), any()))
                .thenThrow(new IllegalStateException("provider details"));

        StepVerifier.create(service.sendSMSCode(
                        phone, CodeTypeEnum.LOGIN, request("10.0.0.20")))
                .expectErrorMessage("验证码发送失败")
                .verify();
        assertThat(cachedCode(phone)).isNull();
    }

    private Mono<Boolean> loginResult(String phone, String code) {
        return service.login(phone, code)
                .subscribeOn(Schedulers.parallel())
                .map(ignored -> true)
                .onErrorReturn(false);
    }

    private void stubExistingUser(String phone) {
        UserInfo user = stubExistingUserWithoutTokenUpdate(phone);
        when(jwtUtils.createJwt(user)).thenReturn("jwt");
        doReturn(Mono.just(true)).when(service).updateTokenByMobile(phone, "jwt");
    }

    private UserInfo stubExistingUserWithoutTokenUpdate(String phone) {
        UserInfo user = new UserInfo();
        user.setMobile(phone);
        user.setUid(1L);
        doReturn(Mono.just(user)).when(service).checkExist(phone);
        return user;
    }

    private ServerHttpRequest request(String ip) {
        return request(ip, null);
    }

    private ServerHttpRequest request(String ip, String forwardedFor) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest
                .post("/auth/sendSMSCode")
                .remoteAddress(new InetSocketAddress(ip, 12345));
        if (forwardedFor != null) {
            builder.header("X-Forwarded-For", forwardedFor);
        }
        return builder.build();
    }

    private String cachedCode(String phone) {
        UserService.SmsChallenge challenge = cachedChallenge(phone);
        return challenge == null ? null : challenge.code();
    }

    private UserService.SmsChallenge cachedChallenge(String phone) {
        return (UserService.SmsChallenge) nativeCache(CaffeineCacheEnum.MOBILE_SMSCODE)
                .getIfPresent(RedisConstant.MOBILE_SMSCODE + phone);
    }

    private void putCode(String phone, String code) {
        nativeCache(CaffeineCacheEnum.MOBILE_SMSCODE)
                .put(RedisConstant.MOBILE_SMSCODE + phone,
                        new UserService.SmsChallenge(code, 0));
    }

    private com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache(
            CaffeineCacheEnum cacheName) {
        Cache cache = cacheManager.getCache(cacheName.name());
        return ((org.springframework.cache.caffeine.CaffeineCache) cache).getNativeCache();
    }
}
