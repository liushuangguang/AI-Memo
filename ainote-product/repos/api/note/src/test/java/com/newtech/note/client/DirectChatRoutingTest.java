package com.newtech.note.client;

import com.newtech.note.service.PointsService;
import com.newtech.note.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DirectChatRoutingTest {
    private DifyClient client() {
        var client = new DifyClient(WebClient.builder().exchangeFunction(request ->
                Mono.error(new AssertionError("Dify must not receive direct-mode traffic"))).build(),
                mock(PointsService.class), mock(UserService.class), "https://example.test", 3, true);
        ReflectionTestUtils.setField(client, "preferDirect", true);
        return client;
    }

    @Test
    void directModeUsesCodeWorkflowOnceForBlockingAndStreaming() {
        var calls = new AtomicInteger();
        var request = MockServerHttpRequest.get("/").header("Authorization", "Guest direct-test")
                .header("Device-Id", "direct-test").build();
        var client = client();
        java.util.function.Supplier<Mono<DeepSeekCompletion>> provider = () -> {
            calls.incrementAndGet();
            return Mono.just(new DeepSeekCompletion("synthetic-result", 10));
        };
        StepVerifier.create(client.callDifyBlockingWorkflowApiWithFallback("synthetic-app", Map.of(),
                "text", request, provider)).expectNext("synthetic-result").verifyComplete();
        StepVerifier.create(client.callDifyStreamingWorkflowApiWithFallback("synthetic-app", Map.of(),
                request, provider)).expectNext("synthetic-result").verifyComplete();
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void directModeNeverBypassesAuthenticationOrReplaysFailure() {
        var calls = new AtomicInteger();
        java.util.function.Supplier<Mono<DeepSeekCompletion>> provider = () -> {
            calls.incrementAndGet();
            return Mono.error(new IllegalStateException("synthetic transport failure"));
        };
        var client = client();
        StepVerifier.create(client.callDifyBlockingWorkflowApiWithFallback("", Map.of(), "text", null,
                provider)).expectError(ResponseStatusException.class).verify();
        assertThat(calls.get()).isZero();
        var request = MockServerHttpRequest.get("/").header("Authorization", "Guest direct-test")
                .header("Device-Id", "direct-test").build();
        StepVerifier.create(client.callDifyBlockingWorkflowApiWithFallback("", Map.of(), "text", request,
                provider)).expectErrorMessage("synthetic transport failure").verify();
        assertThat(calls.get()).isEqualTo(1);
    }
}
