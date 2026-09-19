package com.newtech.note.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BailianClientTest {

    @Test
    void failsBeforeNetworkCallWhenApiKeyIsMissing() {
        AtomicBoolean called = new AtomicBoolean(false);
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    called.set(true);
                    return Mono.error(new AssertionError("Network call must not be attempted"));
                })
                .build();
        BailianClient client = new BailianClient(webClient, new ObjectMapper(), "");

        StepVerifier.create(client.callAppFiltered("test-app", Map.of("value", "test")))
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().contains("BAILIAN_API_KEY"))
                .verify();

        assertFalse(called.get());
    }

    @Test
    void usesConfiguredCredentialWithoutRequiringALiveProvider() {
        AtomicReference<String> authorization = new AtomicReference<>();
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    authorization.set(request.headers().getFirst(HttpHeaders.AUTHORIZATION));
                    ClientResponse response = ClientResponse.create(HttpStatus.OK)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body("{\"output\":{\"finish_reason\":\"stop\",\"session_id\":\"test-session\",\"text\":\"synthetic-result\"},\"request_id\":\"test-request\",\"usage\":{}}")
                            .build();
                    return Mono.just(response);
                })
                .build();
        BailianClient client = new BailianClient(
                webClient, new ObjectMapper(), "test-only-credential");

        StepVerifier.create(client.callAppFiltered("test-app", Map.of("value", "test")))
                .expectNext("synthetic-result")
                .verifyComplete();

        assertEquals("Bearer test-only-credential", authorization.get());
    }
}
