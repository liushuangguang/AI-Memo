package com.newtech.note.client;

import com.newtech.note.common.BusinessException;
import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.entity.dto.PointsChangeEnum;
import com.newtech.note.service.PointsService;
import com.newtech.note.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CozeClientGuestTest {

    @Test
    void guestWorkflowSkipsUserAndPointsLookups() {
        UserService userService = mock(UserService.class);
        PointsService pointsService = mock(PointsService.class);
        WebClient webClient = webClientReturning("""
                {"code":0,"cost":"1","data":"{\\"output\\":\\"ok\\"}","debugUrl":"","msg":"","token":5}
                """);
        CozeClient client = new CozeClient(
                webClient, pointsService, userService, "test-key", "https://coze.test/workflow", true, 1000);

        StepVerifier.create(client.callCozeWorkflowApiFiltered(
                        "workflow-1", "", Map.of("inp", "note"), null, guestRequest()))
                .expectNext("{\"output\":\"ok\"}")
                .verifyComplete();

        verifyNoInteractions(userService, pointsService);
    }

    @Test
    void guestBotSkipsUserAndPointsLookups() {
        UserService userService = mock(UserService.class);
        PointsService pointsService = mock(PointsService.class);
        WebClient webClient = webClientReturning("""
                {"messages":[{"role":"assistant","type":"answer","content":"ok","content_type":"text","extra_info":null}],"code":"0","msg":"","conversation_id":"c1"}
                """);
        CozeClientV2 client = new CozeClientV2(
                webClient, pointsService, userService, "test-key", "https://coze.test/chat", true, 1000);

        StepVerifier.create(client.callCozeBotApiFiltered(
                        "device", "note", "bot-1", guestRequest()))
                .expectNext("ok")
                .verifyComplete();

        verifyNoInteractions(userService, pointsService);
    }

    @Test
    void missingCredentialFailsClearlyBeforeSending() {
        CozeClient client = new CozeClient(
                WebClient.builder().build(), mock(PointsService.class), mock(UserService.class),
                "", "https://coze.test/workflow", true, 1000);

        StepVerifier.create(client.callCozeWorkflowApiFiltered(
                        "workflow-1", "", Map.of(), null, guestRequest()))
                .expectErrorSatisfies(error -> {
                    BusinessException failure = assertInstanceOf(BusinessException.class, error);
                    assertEquals("COZE_NOT_CONFIGURED", failure.getCode());
                })
                .verify();
    }

    @Test
    void neverCompletingWorkflowIsMappedToConfiguredTimeout() {
        CozeClient client = new CozeClient(
                WebClient.builder().exchangeFunction(request -> Mono.never()).build(),
                mock(PointsService.class), mock(UserService.class),
                "test-key", "https://coze.test/workflow", true, 1000, 1);

        StepVerifier.create(client.callCozeWorkflowApiFiltered(
                        "workflow-1", "", Map.of("inp", "query"), null, guestRequest()))
                .expectErrorSatisfies(error -> {
                    BusinessException failure = assertInstanceOf(BusinessException.class, error);
                    assertEquals("COZE_TIMEOUT", failure.getCode());
                })
                .verify(java.time.Duration.ofSeconds(3));
    }

    @Test
    void neverCompletingLegacyBotCallIsMappedToConfiguredTimeout() {
        CozeClientV2 client = new CozeClientV2(
                WebClient.builder().exchangeFunction(request -> Mono.never()).build(),
                mock(PointsService.class), mock(UserService.class),
                "test-key", "https://coze.test/chat", true, 1000, 1);

        StepVerifier.create(client.callCozeBotApiFiltered(
                        "device", "note", "bot-1", guestRequest()))
                .expectErrorSatisfies(error -> {
                    BusinessException failure = assertInstanceOf(BusinessException.class, error);
                    assertEquals("COZE_TIMEOUT", failure.getCode());
                })
                .verify(java.time.Duration.ofSeconds(3));
    }

    @Test
    void guestAuthorizationRequiresFeatureFlagAndExactFormat() {
        UserService userService = mock(UserService.class);
        PointsService pointsService = mock(PointsService.class);
        CozeClient client = new CozeClient(
                webClientReturning("{}"), pointsService, userService,
                "test-key", "https://coze.test/workflow", false, 1000);

        assertBusinessError(client.callCozeWorkflowApiFiltered(
                "workflow-1", "", Map.of(), null, guestRequest()), "UNAUTHORIZED");
        assertBusinessError(client.callCozeWorkflowApiFiltered(
                "workflow-1", "", Map.of(), null, request("Guest device id")), "UNAUTHORIZED");

        CozeClient enabled = new CozeClient(
                webClientReturning("{}"), pointsService, userService,
                "test-key", "https://coze.test/workflow", true, 1000);
        MockServerHttpRequest mismatch = MockServerHttpRequest.post("/test")
                .header(HttpHeaders.AUTHORIZATION, "Guest device-a")
                .header("Device-Id", "device-b")
                .build();
        assertBusinessError(enabled.callCozeWorkflowApiFiltered(
                "workflow-1", "", Map.of(), null, mismatch), "UNAUTHORIZED");

        verifyNoInteractions(userService, pointsService);
    }

    @Test
    void workflowProviderErrorAndNullDataAreNotSuccessfulOrCharged() {
        UserService userService = mobileUser();
        PointsService pointsService = availablePoints();
        CozeClient providerError = new CozeClient(
                webClientReturning("{\"code\":4001,\"msg\":\"workflow failed\"}"), pointsService, userService,
                "test-key", "https://coze.test/workflow", false, 700);

        assertBusinessError(providerError.callCozeWorkflowApiFiltered(
                "workflow-1", "", Map.of(), null, mobileRequest()), "COZE_4001");

        CozeClient nullData = new CozeClient(
                webClientReturning("{\"code\":0,\"data\":null,\"token\":5}"), pointsService, userService,
                "test-key", "https://coze.test/workflow", false, 700);
        assertBusinessError(nullData.callCozeWorkflowApiFiltered(
                "workflow-1", "", Map.of(), null, mobileRequest()), "COZE_MISSING_DATA");

        verify(pointsService, times(2)).getAvailablePoints(42L, null);
        verify(pointsService, never()).updatePoints(
                42L, null, PointsChangeEnum.CONSUMER, null, 5L, null);
    }

    @Test
    void malformedWorkflowResponseIsTypedAndNotCharged() {
        UserService userService = mobileUser();
        PointsService pointsService = availablePoints();
        CozeClient client = new CozeClient(
                webClientReturning("not-json"), pointsService, userService,
                "test-key", "https://coze.test/workflow", false, 700);

        assertBusinessError(client.callCozeWorkflowApiFiltered(
                "workflow-1", "", Map.of(), null, mobileRequest()), "COZE_MALFORMED_RESPONSE");

        verify(pointsService, never()).updatePoints(
                42L, null, PointsChangeEnum.CONSUMER, null, 700L, null);
    }

    @Test
    void mobileWorkflowChecksAndChargesExactlyOnceUsingReportedTokens() {
        UserService userService = mobileUser();
        PointsService pointsService = availablePoints();
        when(pointsService.updatePoints(42L, null, PointsChangeEnum.CONSUMER, null, 5L, null))
                .thenReturn(Mono.just(NoteBaseResponse.success()));
        CozeClient client = new CozeClient(
                webClientReturning("{\"code\":0,\"data\":\"ok\",\"token\":5}"), pointsService, userService,
                "test-key", "https://coze.test/workflow", false, 700);

        StepVerifier.create(client.callCozeWorkflowApiFiltered(
                        "workflow-1", "", Map.of(), null, mobileRequest()))
                .expectNext("ok")
                .verifyComplete();

        verify(userService, times(1)).getUidByToken("jwt-token");
        verify(pointsService, times(1)).getAvailablePoints(42L, null);
        verify(pointsService, times(1)).updatePoints(
                42L, null, PointsChangeEnum.CONSUMER, null, 5L, null);
    }

    @Test
    void mobileWorkflowFailsWhenConsumptionCannotBePersisted() {
        UserService userService = mobileUser();
        PointsService pointsService = availablePoints();
        when(pointsService.updatePoints(42L, null, PointsChangeEnum.CONSUMER, null, 5L, null))
                .thenReturn(Mono.error(new IllegalStateException("database unavailable")));
        CozeClient client = new CozeClient(
                webClientReturning("{\"code\":0,\"data\":\"ok\",\"token\":5}"), pointsService, userService,
                "test-key", "https://coze.test/workflow", false, 700);

        StepVerifier.create(client.callCozeWorkflowApiFiltered(
                        "workflow-1", "", Map.of(), null, mobileRequest()))
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && "database unavailable".equals(error.getMessage()))
                .verify();
    }

    @Test
    void workflowFallsBackToConfiguredChargeWhenUsageIsUnavailable() {
        UserService userService = mobileUser();
        PointsService pointsService = availablePoints();
        when(pointsService.updatePoints(42L, null, PointsChangeEnum.CONSUMER, null, 700L, null))
                .thenReturn(Mono.just(NoteBaseResponse.success()));
        CozeClient client = new CozeClient(
                webClientReturning("{\"code\":0,\"data\":\"ok\",\"token\":0}"), pointsService, userService,
                "test-key", "https://coze.test/workflow", false, 700);

        StepVerifier.create(client.callCozeWorkflowApiFiltered(
                        "workflow-1", "", Map.of(), null, mobileRequest()))
                .expectNext("ok")
                .verifyComplete();

        verify(pointsService, times(1)).updatePoints(
                42L, null, PointsChangeEnum.CONSUMER, null, 700L, null);
    }

    @Test
    void legacyBotUsesConfiguredFixedChargeExactlyOnce() {
        UserService userService = mobileUser();
        PointsService pointsService = availablePoints();
        when(pointsService.updatePoints(42L, null, PointsChangeEnum.CONSUMER, null, 700L, null))
                .thenReturn(Mono.just(NoteBaseResponse.success()));
        CozeClientV2 client = new CozeClientV2(
                webClientReturning("""
                        {"messages":[{"type":"answer","content":"ok"}],"code":"0","msg":""}
                        """), pointsService, userService,
                "test-key", "https://coze.test/chat", false, 700);

        StepVerifier.create(client.callCozeBotApiFiltered(
                        "device", "note", "bot-1", mobileRequest()))
                .expectNext("ok")
                .verifyComplete();

        verify(userService, times(1)).getUidByToken("jwt-token");
        verify(pointsService, times(1)).getAvailablePoints(42L, null);
        verify(pointsService, times(1)).updatePoints(
                42L, null, PointsChangeEnum.CONSUMER, null, 700L, null);
    }

    @Test
    void legacyStreamWithoutAnswerFailsAndDoesNotCharge() {
        UserService userService = mobileUser();
        PointsService pointsService = availablePoints();
        CozeClientV2 client = new CozeClientV2(
                webClientReturning("{\"event\":\"done\"}"), pointsService, userService,
                "test-key", "https://coze.test/chat", false, 700);

        assertBusinessError(client.callStreamCozeBotApiFiltered(
                "device", "note", "bot-1", mobileRequest()).next(), "COZE_EMPTY_RESPONSE");

        verify(pointsService, never()).updatePoints(
                42L, null, PointsChangeEnum.CONSUMER, null, 700L, null);
    }

    @Test
    void legacyStreamErrorEventIsTypedAndDoesNotCharge() {
        UserService userService = mobileUser();
        PointsService pointsService = availablePoints();
        CozeClientV2 client = new CozeClientV2(
                webClientReturning("{\"event\":\"error\",\"msg\":\"provider failed\"}"), pointsService, userService,
                "test-key", "https://coze.test/chat", false, 700);

        assertBusinessError(client.callStreamCozeBotApiFiltered(
                "device", "note", "bot-1", mobileRequest()).next(), "COZE_STREAM_ERROR");

        verify(pointsService, never()).updatePoints(
                42L, null, PointsChangeEnum.CONSUMER, null, 700L, null);
    }

    @Test
    void legacyStreamChargesOnceOnlyAfterAnAnswerCompletes() {
        UserService userService = mobileUser();
        PointsService pointsService = availablePoints();
        when(pointsService.updatePoints(42L, null, PointsChangeEnum.CONSUMER, null, 700L, null))
                .thenReturn(Mono.just(NoteBaseResponse.success()));
        CozeClientV2 client = new CozeClientV2(
                webClientReturningNdjson("""
                        {"event":"message","message":{"type":"answer","content":"first"}}
                        {"event":"message","message":{"type":"answer","content":"second"}}
                        """), pointsService, userService,
                "test-key", "https://coze.test/chat", false, 700);

        StepVerifier.create(client.callStreamCozeBotApiFiltered(
                        "device", "note", "bot-1", mobileRequest()))
                .expectNext("first", "second")
                .verifyComplete();

        verify(userService, times(1)).getUidByToken("jwt-token");
        verify(pointsService, times(1)).getAvailablePoints(42L, null);
        verify(pointsService, times(1)).updatePoints(
                42L, null, PointsChangeEnum.CONSUMER, null, 700L, null);
    }

    @Test
    void legacyStreamTakeOneStillChargesExactlyOnceBeforeCancellation() {
        UserService userService = mobileUser();
        PointsService pointsService = availablePoints();
        when(pointsService.updatePoints(42L, null, PointsChangeEnum.CONSUMER, null, 700L, null))
                .thenReturn(Mono.just(NoteBaseResponse.success()));
        CozeClientV2 client = new CozeClientV2(
                webClientReturningNdjson("""
                        {"event":"message","message":{"type":"answer","content":"first"}}
                        {"event":"message","message":{"type":"answer","content":"second"}}
                        """), pointsService, userService,
                "test-key", "https://coze.test/chat", false, 700);

        StepVerifier.create(client.callStreamCozeBotApiFiltered(
                        "device", "note", "bot-1", mobileRequest()).take(1))
                .expectNext("first")
                .verifyComplete();

        verify(userService, times(1)).getUidByToken("jwt-token");
        verify(pointsService, times(1)).getAvailablePoints(42L, null);
        verify(pointsService, times(1)).updatePoints(
                42L, null, PointsChangeEnum.CONSUMER, null, 700L, null);
    }

    private MockServerHttpRequest guestRequest() {
        return request("Guest local-device");
    }

    private MockServerHttpRequest mobileRequest() {
        return request("Mobilejwt-token");
    }

    private MockServerHttpRequest request(String authorization) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.post("/test")
                .header(HttpHeaders.AUTHORIZATION, authorization);
        if (authorization.startsWith("Guest ") && !authorization.substring("Guest ".length()).contains(" ")) {
            builder.header("Device-Id", authorization.substring("Guest ".length()));
        }
        return builder.build();
    }

    private UserService mobileUser() {
        UserService userService = mock(UserService.class);
        when(userService.getUidByToken("jwt-token")).thenReturn(Mono.just(42L));
        return userService;
    }

    private PointsService availablePoints() {
        PointsService pointsService = mock(PointsService.class);
        when(pointsService.getAvailablePoints(42L, null)).thenReturn(Mono.just(10.0));
        return pointsService;
    }

    private void assertBusinessError(org.reactivestreams.Publisher<?> publisher, String code) {
        StepVerifier.create(publisher)
                .expectErrorSatisfies(error -> {
                    BusinessException failure = assertInstanceOf(BusinessException.class, error);
                    assertEquals(code, failure.getCode());
                })
                .verify();
    }

    private WebClient webClientReturning(String json) {
        return webClientReturning(json, MediaType.APPLICATION_JSON_VALUE);
    }

    private WebClient webClientReturningNdjson(String json) {
        return webClientReturning(json, MediaType.APPLICATION_NDJSON_VALUE);
    }

    private WebClient webClientReturning(String json, String contentType) {
        return WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .body(json)
                        .build()))
                .build();
    }
}
