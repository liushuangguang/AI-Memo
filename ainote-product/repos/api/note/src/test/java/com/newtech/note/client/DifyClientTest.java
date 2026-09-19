package com.newtech.note.client;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.common.BusinessException;
import com.newtech.note.entity.dto.PointsChangeEnum;
import com.newtech.note.service.PointsService;
import com.newtech.note.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class DifyClientTest {

    @Test
    void enabledGuestUsesConfiguredOfficialEndpointWithoutUserLookup() {
        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        ExchangeFunction exchange = request -> {
            captured.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body("""
                            {"workflow_run_id":"w","task_id":"t","data":{"status":"succeeded",
                            "outputs":{"res":"```json\\n{\\\"result\\\":\\\"1\\\"}\\n```"},"total_tokens":5}}
                            """)
                    .build());
        };
        PointsService pointsService = mock(PointsService.class);
        UserService userService = mock(UserService.class);
        DifyClient client = client(exchange, pointsService, userService, true);

        StepVerifier.create(client.callDifyBlockingWorkflowApiFiltered(
                        "app-test", Map.of("inp", "note"), "res", guestRequest("test-device")))
                .expectNext("{\"result\":\"1\"}")
                .verifyComplete();

        assertThat(captured.get().url().toString())
                .isEqualTo("https://api.dify.ai/v1/workflows/run");
        assertThat(captured.get().headers().getFirst("Authorization"))
                .isEqualTo("Bearer app-test");
        verifyNoInteractions(pointsService, userService);
    }

    @Test
    void anonymousRequestIsRejectedBeforeProviderOrFallback() {
        PointsService pointsService = mock(PointsService.class);
        UserService userService = mock(UserService.class);
        AtomicBoolean fallbackCalled = new AtomicBoolean();
        DifyClient client = client(request -> Mono.error(
                new AssertionError("network must not be called")), pointsService, userService, true);

        StepVerifier.create(client.callDifyBlockingWorkflowApiWithFallback(
                        "", Map.of("inp", "note"), "res", null,
                        () -> {
                            fallbackCalled.set(true);
                            return Mono.just(new DeepSeekCompletion("fallback", 10));
                        }))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ResponseStatusException.class);
                    assertThat(((ResponseStatusException) error).getStatusCode())
                            .isEqualTo(HttpStatus.UNAUTHORIZED);
                })
                .verify();

        assertThat(fallbackCalled).isFalse();
        verifyNoInteractions(pointsService, userService);
    }

    @Test
    void guestAuthorizationIsRejectedWhenGuestModeIsDisabled() {
        PointsService pointsService = mock(PointsService.class);
        UserService userService = mock(UserService.class);
        DifyClient client = client(request -> Mono.error(
                new AssertionError("network must not be called")), pointsService, userService, false);

        StepVerifier.create(client.callDifyBlockingWorkflowApiFiltered(
                        "app-test", Map.of("inp", "note"), "res", guestRequest("test-device")))
                .expectError(ResponseStatusException.class)
                .verify();
        verifyNoInteractions(pointsService, userService);
    }

    @Test
    void arbitraryNonMobileAuthorizationIsRejected() {
        PointsService pointsService = mock(PointsService.class);
        UserService userService = mock(UserService.class);
        DifyClient client = client(request -> Mono.error(
                new AssertionError("network must not be called")), pointsService, userService, true);
        ServerHttpRequest bearer = MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer unrelated-token")
                .build();

        StepVerifier.create(client.callDifyBlockingWorkflowApiFiltered(
                        "app-test", Map.of("inp", "note"), "res", bearer))
                .expectError(ResponseStatusException.class)
                .verify();
        verifyNoInteractions(pointsService, userService);
    }

    @Test
    void malformedGuestAuthorizationIsRejected() {
        PointsService pointsService = mock(PointsService.class);
        UserService userService = mock(UserService.class);
        DifyClient client = client(request -> Mono.error(
                new AssertionError("network must not be called")), pointsService, userService, true);
        ServerHttpRequest malformed = MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Guest device-id ")
                .build();

        StepVerifier.create(client.callDifyBlockingWorkflowApiFiltered(
                        "app-test", Map.of("inp", "note"), "res", malformed))
                .expectError(ResponseStatusException.class)
                .verify();
        verifyNoInteractions(pointsService, userService);
    }

    @Test
    void mismatchedGuestDeviceIsRejectedBeforeProviderAndPersistence() {
        PointsService pointsService = mock(PointsService.class);
        UserService userService = mock(UserService.class);
        DifyClient client = client(request -> Mono.error(
                new AssertionError("network must not be called")), pointsService, userService, true);
        ServerHttpRequest mismatched = MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Guest device-a")
                .header("Device-Id", "device-b")
                .build();

        StepVerifier.create(client.callDifyBlockingWorkflowApiFiltered(
                        "app-test", Map.of("inp", "note"), "res", mismatched))
                .expectError(ResponseStatusException.class)
                .verify();
        verifyNoInteractions(pointsService, userService);
    }

    @Test
    void mobileFallbackValidatesAndDeductsProviderUsageExactlyOnce() {
        PointsService pointsService = mock(PointsService.class);
        UserService userService = mock(UserService.class);
        when(userService.getUidByToken("jwt-token")).thenReturn(Mono.just(42L));
        when(pointsService.getAvailablePoints(42L, null)).thenReturn(Mono.just(10.0));
        when(pointsService.updatePoints(eq(42L), isNull(), eq(PointsChangeEnum.CONSUMER),
                isNull(), eq(120L), isNull()))
                .thenReturn(Mono.just(NoteBaseResponse.<Void>success()));
        DifyClient client = client(request -> Mono.error(
                new AssertionError("Dify network must not be called without a key")),
                pointsService, userService, false);

        StepVerifier.create(client.callDifyBlockingWorkflowApiWithFallback(
                        "", Map.of("inp", "note"), "res", mobileRequest("jwt-token"),
                        () -> Mono.just(new DeepSeekCompletion("fallback", 120))))
                .expectNext("fallback")
                .verifyComplete();

        verify(userService, times(1)).getUidByToken("jwt-token");
        verify(pointsService, times(1)).getAvailablePoints(42L, null);
        verify(pointsService, times(1)).updatePoints(
                42L, null, PointsChangeEnum.CONSUMER, null, 120L, null);
    }

    @Test
    void ordinaryDifyFallbackStillUsesSanitizedDeepSeekContent() {
        DifyClient client = client(request -> Mono.error(
                        new AssertionError("Dify network must not be called without a key")),
                mock(PointsService.class), mock(UserService.class), true);

        StepVerifier.create(client.callDifyBlockingWorkflowApiWithFallback(
                        "", Map.of("inp", "note"), "res", guestRequest("guest-a"),
                        () -> Mono.just(new DeepSeekCompletion(
                                "{\"result\":\"clean\"}",
                                "```json\n{\"result\":\"clean\"}\n```",
                                12))))
                .expectNext("{\"result\":\"clean\"}")
                .verifyComplete();
    }

    @Test
    void mobileHttp401FallbackAlsoValidatesAndDeductsExactlyOnce() {
        PointsService pointsService = mock(PointsService.class);
        UserService userService = mock(UserService.class);
        when(userService.getUidByToken("jwt-token")).thenReturn(Mono.just(42L));
        when(pointsService.getAvailablePoints(42L, null)).thenReturn(Mono.just(10.0));
        when(pointsService.updatePoints(eq(42L), isNull(), eq(PointsChangeEnum.CONSUMER),
                isNull(), eq(80L), isNull()))
                .thenReturn(Mono.just(NoteBaseResponse.<Void>success()));
        DifyClient client = client(request ->
                Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED).build()),
                pointsService, userService, false);

        StepVerifier.create(client.callDifyBlockingWorkflowApiWithFallback(
                        "app-expired", Map.of("inp", "note"), "res", mobileRequest("jwt-token"),
                        () -> Mono.just(new DeepSeekCompletion("fallback", 80))))
                .expectNext("fallback")
                .verifyComplete();

        verify(userService, times(1)).getUidByToken("jwt-token");
        verify(pointsService, times(1)).getAvailablePoints(42L, null);
        verify(pointsService, times(1)).updatePoints(
                42L, null, PointsChangeEnum.CONSUMER, null, 80L, null);
    }

    @Test
    void failedWorkflowStatusUsesFallbackInsteadOfReturningOutputs() {
        DifyClient client = client(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body("""
                                {"workflow_run_id":"w","task_id":"t","data":{"status":"failed",
                                 "outputs":{"suggestion":"partial"},"error":"provider billing failure"}}
                                """)
                        .build()),
                mock(PointsService.class), mock(UserService.class), true);

        StepVerifier.create(client.callDifyBlockingWorkflowApiWithFallback(
                        "app-test", Map.of("note", "private"), "suggestion", guestRequest("guest-a"),
                        () -> Mono.just(new DeepSeekCompletion("fallback", 5))))
                .expectNext("fallback")
                .verifyComplete();
    }

    @Test
    void mobileFallbackDoesNotRunWhenPointsValidationFails() {
        PointsService pointsService = mock(PointsService.class);
        UserService userService = mock(UserService.class);
        AtomicBoolean fallbackCalled = new AtomicBoolean();
        when(userService.getUidByToken("jwt-token")).thenReturn(Mono.just(42L));
        when(pointsService.getAvailablePoints(42L, null)).thenReturn(Mono.just(0.0));
        DifyClient client = client(request -> Mono.error(
                new AssertionError("provider must not be called")), pointsService, userService, false);

        StepVerifier.create(client.callDifyBlockingWorkflowApiWithFallback(
                        "", Map.of("inp", "note"), "res", mobileRequest("jwt-token"),
                        () -> {
                            fallbackCalled.set(true);
                            return Mono.just(new DeepSeekCompletion("fallback", 120));
                        }))
                .expectError(BusinessException.class)
                .verify();

        assertThat(fallbackCalled).isFalse();
        verify(pointsService, never()).updatePoints(
                eq(42L), isNull(), eq(PointsChangeEnum.CONSUMER),
                isNull(), eq(120L), isNull());
    }

    @Test
    void missingWorkflowKeyIsTypedProviderFailureForAuthorizedGuest() {
        PointsService pointsService = mock(PointsService.class);
        UserService userService = mock(UserService.class);
        DifyClient client = client(request -> Mono.error(
                new AssertionError("network must not be called")), pointsService, userService, true);

        StepVerifier.create(client.callDifyBlockingWorkflowApiFiltered(
                        "", Map.of("inp", "note"), "res", guestRequest("test-device")))
                .expectError(DifyProviderException.class)
                .verify();
        verifyNoInteractions(pointsService, userService);
    }

    @Test
    void authenticationFailureIsTypedForAuthorizedGuest() {
        PointsService pointsService = mock(PointsService.class);
        UserService userService = mock(UserService.class);
        DifyClient client = client(request ->
                Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED).build()),
                pointsService, userService, true);

        StepVerifier.create(client.callDifyBlockingWorkflowApiFiltered(
                        "app-expired", Map.of("inp", "note"), "res", guestRequest("test-device")))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(DifyProviderException.class);
                    assertThat(error.getMessage()).contains("HTTP 401");
                })
                .verify();
        verifyNoInteractions(pointsService, userService);
    }

    @Test
    void blockingUsagePersistenceFailureFailsTheRequest() {
        PointsService pointsService = mock(PointsService.class);
        UserService userService = mock(UserService.class);
        when(userService.getUidByToken("jwt-token")).thenReturn(Mono.just(42L));
        when(pointsService.getAvailablePoints(42L, null)).thenReturn(Mono.just(10.0));
        when(pointsService.updatePoints(42L, null, PointsChangeEnum.CONSUMER, null, 5L, null))
                .thenReturn(Mono.error(new IllegalStateException("database unavailable")));
        DifyClient client = client(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body("""
                                {"workflow_run_id":"w","task_id":"t","data":{"status":"succeeded",
                                "outputs":{"res":"answer"},"total_tokens":5}}
                                """)
                        .build()), pointsService, userService, false);

        StepVerifier.create(client.callDifyBlockingWorkflowApiFiltered(
                        "app-test", Map.of("inp", "private note"), "res", mobileRequest("jwt-token")))
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && "database unavailable".equals(error.getMessage()))
                .verify();
    }

    @Test
    void streamingUsagePersistenceFailureIsObservedBeforeNormalCompletion() {
        PointsService pointsService = mock(PointsService.class);
        UserService userService = mock(UserService.class);
        when(userService.getUidByToken("jwt-token")).thenReturn(Mono.just(42L));
        when(pointsService.getAvailablePoints(42L, null)).thenReturn(Mono.just(10.0));
        when(pointsService.updatePoints(42L, null, PointsChangeEnum.CONSUMER, null, 5L, null))
                .thenReturn(Mono.error(new IllegalStateException("database unavailable")));
        ExchangeFunction exchange = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_NDJSON_VALUE)
                .body("""
                        {"event":"text_chunk","workflow_run_id":"w","task_id":"t",
                         "data":{"text":"answer","from_variable_selector":[],
                         "execution_metadata":{"total_tokens":"5"}}}
                        {"event":"workflow_finished","workflow_run_id":"w","task_id":"t",
                         "data":{"status":"succeeded","total_tokens":5}}
                        """)
                .build());
        DifyClient client = client(exchange, pointsService, userService, false);

        StepVerifier.create(client.callDifyStreamingWorkflowApiFiltered(
                        "app-test", Map.of("inp", "private note"), mobileRequest("jwt-token")))
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && "database unavailable".equals(error.getMessage()))
                .verify();

        verify(pointsService, times(1)).updatePoints(
                42L, null, PointsChangeEnum.CONSUMER, null, 5L, null);
    }

    @Test
    void guestStreamingFallbackSuppressesPartialOutputWhenProviderStreamBreaks() {
        ExchangeFunction exchange = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_NDJSON_VALUE)
                .body("""
                        {"event":"text_chunk","workflow_run_id":"w","task_id":"t",
                         "data":{"text":"partial","from_variable_selector":[],"execution_metadata":null}}
                        {"event":
                        """)
                .build());
        DifyClient client = client(exchange, mock(PointsService.class), mock(UserService.class), true);

        StepVerifier.create(client.callDifyStreamingWorkflowApiWithFallback(
                "app-test", Map.of("note", "private"), guestRequest("guest-a"),
                        () -> Mono.just(new DeepSeekCompletion("fallback", 10))))
                .expectNext("fallback")
                .verifyComplete();
    }

    @Test
    void mobileBlockingSuccessFailsClosedForMissingNonNumericNonPositiveOrOverflowUsage() {
        for (String usageField : new String[]{
                "",
                ",\"total_tokens\":\"five\"",
                ",\"total_tokens\":0",
                ",\"total_tokens\":-1",
                ",\"total_tokens\":9223372036854775808"
        }) {
            PointsService pointsService = mock(PointsService.class);
            UserService userService = mock(UserService.class);
            when(userService.getUidByToken("jwt-token")).thenReturn(Mono.just(42L));
            when(pointsService.getAvailablePoints(42L, null)).thenReturn(Mono.just(10.0));
            String body = "{\"data\":{\"status\":\"succeeded\",\"outputs\":{\"res\":\"answer\"}"
                    + usageField + "}}";
            DifyClient client = client(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .body(body)
                            .build()), pointsService, userService, false);

            StepVerifier.create(client.callDifyBlockingWorkflowApiFiltered(
                            "app-test", Map.of(), "res", mobileRequest("jwt-token")))
                    .expectErrorSatisfies(error -> assertThat(error)
                            .isInstanceOfSatisfying(BusinessException.class,
                                    failure -> assertThat(failure.getCode()).isEqualTo("DIFY_USAGE_INVALID")))
                    .verify();

            verify(pointsService, never()).updatePoints(
                    eq(42L), isNull(), eq(PointsChangeEnum.CONSUMER),
                    isNull(), org.mockito.ArgumentMatchers.anyLong(), isNull());
        }
    }

    @Test
    void guestBlockingSuccessMayCompleteWithoutUsage() {
        DifyClient client = client(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"data\":{\"status\":\"succeeded\",\"outputs\":{\"res\":\"answer\"}}}")
                        .build()), mock(PointsService.class), mock(UserService.class), true);

        StepVerifier.create(client.callDifyBlockingWorkflowApiFiltered(
                        "app-test", Map.of(), "res", guestRequest("guest-a")))
                .expectNext("answer")
                .verifyComplete();
    }

    @Test
    void mobileStreamingSuccessDoesNotNormallyCompleteWithInvalidUsage() {
        for (String usage : new String[]{"null", "\"not-a-number\"", "\"0\"",
                "\"-1\"", "\"9223372036854775808\""}) {
            PointsService pointsService = mock(PointsService.class);
            UserService userService = mock(UserService.class);
            when(userService.getUidByToken("jwt-token")).thenReturn(Mono.just(42L));
            when(pointsService.getAvailablePoints(42L, null)).thenReturn(Mono.just(10.0));
            String body = "{\"event\":\"text_chunk\",\"data\":{\"text\":\"answer\","
                    + "\"from_variable_selector\":[],"
                    + "\"execution_metadata\":{\"total_tokens\":\"999\"}}}\n"
                    + "{\"event\":\"workflow_finished\",\"data\":{\"status\":\"succeeded\","
                    + "\"total_tokens\":" + usage + "}}";
            DifyClient client = client(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                            .header("Content-Type", MediaType.APPLICATION_NDJSON_VALUE)
                            .body(body)
                            .build()), pointsService, userService, false);

            StepVerifier.create(client.callDifyStreamingWorkflowApiFiltered(
                            "app-test", Map.of(), mobileRequest("jwt-token")))
                    .expectErrorSatisfies(error -> assertThat(error)
                            .isInstanceOfSatisfying(BusinessException.class,
                                    failure -> assertThat(failure.getCode()).isEqualTo("DIFY_USAGE_INVALID")))
                    .verify();

            verify(pointsService, never()).updatePoints(
                    eq(42L), isNull(), eq(PointsChangeEnum.CONSUMER),
                    isNull(), org.mockito.ArgumentMatchers.anyLong(), isNull());
        }
    }

    @Test
    void mobileStreamingBillsOnlyAuthoritativeSuccessfulTerminalUsageBeforeEmitting() {
        PointsService pointsService = mock(PointsService.class);
        UserService userService = mock(UserService.class);
        AtomicBoolean billed = new AtomicBoolean();
        when(userService.getUidByToken("jwt-token")).thenReturn(Mono.just(42L));
        when(pointsService.getAvailablePoints(42L, null)).thenReturn(Mono.just(10.0));
        when(pointsService.updatePoints(42L, null, PointsChangeEnum.CONSUMER, null, 7L, null))
                .thenReturn(Mono.fromRunnable(() -> billed.set(true))
                        .thenReturn(NoteBaseResponse.<Void>success()));
        String body = """
                {"event":"node_finished","data":{"text":"not-text","execution_metadata":{"total_tokens":"900"}}}
                {"event":"text_chunk","data":{"text":"first","execution_metadata":{"total_tokens":"100"}}}
                {"event":"text_chunk","data":{"text":"second","execution_metadata":{"total_tokens":"200"}}}
                {"event":"workflow_finished","data":{"status":"succeeded","total_tokens":7}}
                """;
        DifyClient client = client(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_NDJSON_VALUE)
                        .body(body)
                        .build()), pointsService, userService, false);

        StepVerifier.create(client.callDifyStreamingWorkflowApiFiltered(
                        "app-test", Map.of(), mobileRequest("jwt-token")))
                .expectNextMatches(chunk -> billed.get() && "first".equals(chunk))
                .expectNext("second")
                .verifyComplete();

        verify(pointsService).updatePoints(
                42L, null, PointsChangeEnum.CONSUMER, null, 7L, null);
    }

    @Test
    void failedWorkflowTerminalEmitsNothingAndIsProviderFailure() {
        DifyClient client = client(streamingExchange("""
                        {"event":"text_chunk","data":{"text":"partial"}}
                        {"event":"workflow_finished","data":{"status":"failed","total_tokens":9}}
                        """), mobilePoints(), mobileUser(), false);

        StepVerifier.create(client.callDifyStreamingWorkflowApiFiltered(
                        "app-test", Map.of(), mobileRequest("jwt-token")))
                .expectError(DifyProviderException.class)
                .verify();
    }

    @Test
    void failedWorkflowTerminalUsesFallbackAndBillsOnlyFallbackUsage() {
        PointsService pointsService = mobilePoints();
        UserService userService = mobileUser();
        when(pointsService.updatePoints(42L, null, PointsChangeEnum.CONSUMER, null, 11L, null))
                .thenReturn(Mono.just(NoteBaseResponse.<Void>success()));
        DifyClient client = client(streamingExchange("""
                        {"event":"text_chunk","data":{"text":"partial"}}
                        {"event":"workflow_finished","data":{"status":"canceled","total_tokens":9}}
                        """), pointsService, userService, false);

        StepVerifier.create(client.callDifyStreamingWorkflowApiWithFallback(
                        "app-test", Map.of(), mobileRequest("jwt-token"),
                        () -> Mono.just(new DeepSeekCompletion("fallback", 11))))
                .expectNext("fallback")
                .verifyComplete();

        verify(pointsService).updatePoints(
                42L, null, PointsChangeEnum.CONSUMER, null, 11L, null);
        verify(pointsService, never()).updatePoints(
                42L, null, PointsChangeEnum.CONSUMER, null, 9L, null);
    }

    @Test
    void missingOrDuplicateWorkflowTerminalEmitsNothing() {
        for (String body : new String[]{
                "{\"event\":\"text_chunk\",\"data\":{\"text\":\"partial\"}}",
                """
                        {"event":"text_chunk","data":{"text":"partial"}}
                        {"event":"workflow_finished","data":{"status":"succeeded","total_tokens":5}}
                        {"event":"workflow_finished","data":{"status":"succeeded","total_tokens":6}}
                        """
        }) {
            DifyClient client = client(streamingExchange(body), mobilePoints(), mobileUser(), false);

            StepVerifier.create(client.callDifyStreamingWorkflowApiFiltered(
                            "app-test", Map.of(), mobileRequest("jwt-token")))
                    .expectError(DifyProviderException.class)
                    .verify();
        }
    }

    @Test
    void mobileStreamingCancellationBeforeCompletionEmitsNothingAndDoesNotBill() {
        PointsService pointsService = mock(PointsService.class);
        UserService userService = mock(UserService.class);
        when(userService.getUidByToken("jwt-token")).thenReturn(Mono.just(42L));
        when(pointsService.getAvailablePoints(42L, null)).thenReturn(Mono.just(10.0));
        DifyClient client = client(request -> Mono.just(nonCompletingStreamingResponse(
                        "{\"event\":\"text_chunk\",\"data\":{\"text\":\"secret answer\","
                                + "\"from_variable_selector\":[],"
                                + "\"execution_metadata\":{\"total_tokens\":\"5\"}}}")),
                pointsService, userService, false);

        StepVerifier.create(client.callDifyStreamingWorkflowApiFiltered(
                        "app-test", Map.of(), mobileRequest("jwt-token")))
                .expectSubscription()
                .expectNoEvent(Duration.ofMillis(100))
                .thenCancel()
                .verify();

        verify(pointsService, never()).updatePoints(
                eq(42L), isNull(), eq(PointsChangeEnum.CONSUMER),
                isNull(), org.mockito.ArgumentMatchers.anyLong(), isNull());
    }

    @Test
    void guestStreamingEmitsImmediatelyWithoutUsageOrPointsAccess() {
        PointsService pointsService = mock(PointsService.class);
        UserService userService = mock(UserService.class);
        DifyClient client = client(request -> Mono.just(nonCompletingStreamingResponse(
                        "{\"event\":\"node_finished\",\"data\":{\"text\":\"must-not-emit\"}}\n"
                                + "{\"event\":\"text_chunk\",\"data\":{\"text\":\"live\","
                                + "\"from_variable_selector\":[],\"execution_metadata\":null}}")),
                pointsService, userService, true);

        StepVerifier.create(client.callDifyStreamingWorkflowApiFiltered(
                        "app-test", Map.of(), guestRequest("guest-a")))
                .expectNext("live")
                .thenCancel()
                .verify();

        verifyNoInteractions(pointsService, userService);
    }

    @Test
    void guestFilteredStreamingRequiresOneSuccessfulTerminalAndIgnoresNodeMetadata() {
        PointsService pointsService = mock(PointsService.class);
        UserService userService = mock(UserService.class);
        DifyClient client = client(streamingExchange("""
                        {"event":"node_finished","data":{"text":"ignore-me","execution_metadata":{"nested":[1,2,3]}}}
                        {"event":"text_chunk","data":{"text":"first","execution_metadata":{"total_tokens":{"unexpected":true}}}}
                        {"event":"text_chunk","data":{"text":"second","from_variable_selector":{"unexpected":true}}}
                        {"event":"workflow_finished","data":{"status":"succeeded"}}
                        """), pointsService, userService, true);

        StepVerifier.create(client.callDifyStreamingWorkflowApiFiltered(
                        "app-test", Map.of(), guestRequest("guest-a")))
                .expectNext("first", "second")
                .verifyComplete();

        verifyNoInteractions(pointsService, userService);
    }

    @Test
    void guestFilteredStreamingEmitsRealtimeChunksThenErrorsForFailedOrCanceledTerminal() {
        for (String status : new String[]{"failed", "canceled"}) {
            DifyClient client = client(streamingExchange("""
                            {"event":"text_chunk","data":{"text":"partial"}}
                            {"event":"workflow_finished","data":{"status":"%s"}}
                            """.formatted(status)), mock(PointsService.class), mock(UserService.class), true);

            StepVerifier.create(client.callDifyStreamingWorkflowApiFiltered(
                            "app-test", Map.of(), guestRequest("guest-a")))
                    .expectNext("partial")
                    .expectError(DifyProviderException.class)
                    .verify();
        }
    }

    @Test
    void guestFilteredStreamingErrorsForMissingOrDuplicateTerminal() {
        for (String body : new String[]{
                "{\"event\":\"text_chunk\",\"data\":{\"text\":\"partial\"}}",
                """
                        {"event":"text_chunk","data":{"text":"partial"}}
                        {"event":"workflow_finished","data":{"status":"succeeded"}}
                        {"event":"workflow_finished","data":{"status":"succeeded"}}
                        """
        }) {
            DifyClient client = client(streamingExchange(body),
                    mock(PointsService.class), mock(UserService.class), true);

            StepVerifier.create(client.callDifyStreamingWorkflowApiFiltered(
                            "app-test", Map.of(), guestRequest("guest-a")))
                    .expectNext("partial")
                    .expectError(DifyProviderException.class)
                    .verify();
        }
    }

    @Test
    void guestFallbackStreamingReturnsBufferedChunksOnlyAfterSuccessfulTerminal() {
        AtomicBoolean fallbackCalled = new AtomicBoolean();
        DifyClient client = client(streamingExchange("""
                        {"event":"node_finished","data":{"execution_metadata":{"total_tokens":["unexpected"]}}}
                        {"event":"text_chunk","data":{"text":"first","execution_metadata":42}}
                        {"event":"text_chunk","data":{"text":"second"}}
                        {"event":"workflow_finished","data":{"status":"succeeded"}}
                        """), mock(PointsService.class), mock(UserService.class), true);

        StepVerifier.create(client.callDifyStreamingWorkflowApiWithFallback(
                        "app-test", Map.of(), guestRequest("guest-a"), () -> {
                            fallbackCalled.set(true);
                            return Mono.just(new DeepSeekCompletion("fallback", 10));
                        }))
                .expectNext("first", "second")
                .verifyComplete();

        assertThat(fallbackCalled).isFalse();
    }

    @Test
    void guestFallbackStreamingDoesNotEmitPartialBeforeTerminal() {
        AtomicBoolean fallbackCalled = new AtomicBoolean();
        DifyClient client = client(request -> Mono.just(nonCompletingStreamingResponse(
                        "{\"event\":\"text_chunk\",\"data\":{\"text\":\"partial\"}}")),
                mock(PointsService.class), mock(UserService.class), true);

        StepVerifier.create(client.callDifyStreamingWorkflowApiWithFallback(
                        "app-test", Map.of(), guestRequest("guest-a"), () -> {
                            fallbackCalled.set(true);
                            return Mono.just(new DeepSeekCompletion("fallback", 10));
                        }))
                .expectSubscription()
                .expectNoEvent(Duration.ofMillis(100))
                .thenCancel()
                .verify();

        assertThat(fallbackCalled).isFalse();
    }

    @Test
    void guestFallbackStreamingSuppressesPartialForFailedOrCanceledTerminal() {
        for (String status : new String[]{"failed", "canceled"}) {
            DifyClient client = client(streamingExchange("""
                            {"event":"text_chunk","data":{"text":"partial"}}
                            {"event":"workflow_finished","data":{"status":"%s"}}
                            """.formatted(status)), mock(PointsService.class), mock(UserService.class), true);

            StepVerifier.create(client.callDifyStreamingWorkflowApiWithFallback(
                            "app-test", Map.of(), guestRequest("guest-a"),
                            () -> Mono.just(new DeepSeekCompletion("fallback", 10))))
                    .expectNext("fallback")
                    .verifyComplete();
        }
    }

    @Test
    void guestFallbackStreamingSuppressesPartialForMissingOrDuplicateTerminal() {
        for (String body : new String[]{
                "{\"event\":\"text_chunk\",\"data\":{\"text\":\"partial\"}}",
                """
                        {"event":"text_chunk","data":{"text":"partial"}}
                        {"event":"workflow_finished","data":{"status":"succeeded"}}
                        {"event":"workflow_finished","data":{"status":"succeeded"}}
                        """
        }) {
            DifyClient client = client(streamingExchange(body),
                    mock(PointsService.class), mock(UserService.class), true);

            StepVerifier.create(client.callDifyStreamingWorkflowApiWithFallback(
                            "app-test", Map.of(), guestRequest("guest-a"),
                            () -> Mono.just(new DeepSeekCompletion("fallback", 10))))
                    .expectNext("fallback")
                    .verifyComplete();
        }
    }

    @Test
    void mobileStreamingFallbackBillsBeforeEmittingAndSuppressesOutputOnDbFailure() {
        PointsService pointsService = mock(PointsService.class);
        UserService userService = mock(UserService.class);
        when(userService.getUidByToken("jwt-token")).thenReturn(Mono.just(42L));
        when(pointsService.getAvailablePoints(42L, null)).thenReturn(Mono.just(10.0));
        when(pointsService.updatePoints(42L, null, PointsChangeEnum.CONSUMER, null, 7L, null))
                .thenReturn(Mono.error(new IllegalStateException("database unavailable")));
        DifyClient client = client(request -> Mono.error(new AssertionError("network must not run")),
                pointsService, userService, false);

        StepVerifier.create(client.callDifyStreamingWorkflowApiWithFallback(
                        "", Map.of(), mobileRequest("jwt-token"),
                        () -> Mono.just(new DeepSeekCompletion("fallback", 7))))
                .expectErrorMessage("database unavailable")
                .verify();
    }

    private ClientResponse nonCompletingStreamingResponse(String firstEvent) {
        return ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_NDJSON_VALUE)
                .body(Flux.concat(
                        Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(
                                firstEvent.getBytes(StandardCharsets.UTF_8))),
                        Flux.never()))
                .build();
    }

    private ExchangeFunction streamingExchange(String body) {
        return request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_NDJSON_VALUE)
                .body(body)
                .build());
    }

    private PointsService mobilePoints() {
        PointsService pointsService = mock(PointsService.class);
        when(pointsService.getAvailablePoints(42L, null)).thenReturn(Mono.just(10.0));
        return pointsService;
    }

    private UserService mobileUser() {
        UserService userService = mock(UserService.class);
        when(userService.getUidByToken("jwt-token")).thenReturn(Mono.just(42L));
        return userService;
    }

    private DifyClient client(ExchangeFunction exchange,
                              PointsService pointsService,
                              UserService userService,
                              boolean guestEnabled) {
        return new DifyClient(
                WebClient.builder().exchangeFunction(exchange).build(),
                pointsService,
                userService,
                "https://api.dify.ai/",
                5,
                guestEnabled);
    }

    private ServerHttpRequest guestRequest(String deviceId) {
        return MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Guest " + deviceId)
                .header("Device-Id", deviceId)
                .build();
    }

    private ServerHttpRequest mobileRequest(String token) {
        return MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Mobile" + token)
                .build();
    }
}
