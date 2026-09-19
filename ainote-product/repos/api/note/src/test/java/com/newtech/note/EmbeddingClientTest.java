package com.newtech.note;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtech.note.client.SiliconFlowEmbeddingClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddingClientTest {

    @Test
    void failsBeforeNetworkCallWhenApiKeyIsMissing() {
        AtomicReference<Boolean> called = new AtomicReference<>(false);
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    called.set(true);
                    return Mono.error(new AssertionError("Network call must not be attempted"));
                })
                .build();
        SiliconFlowEmbeddingClient client = new SiliconFlowEmbeddingClient(
                webClient,
                new ObjectMapper(),
                "https://example.invalid/v1/embeddings",
                "",
                "test-model");

        StepVerifier.create(client.getEmbedding("test note"))
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().contains("SILICONFLOW_API_KEY"))
                .verify();

        assertEquals(false, called.get());
    }

    @Test
    void readsConfiguredCredentialAndParsesResponse() {
        AtomicReference<String> authorization = new AtomicReference<>();
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    authorization.set(request.headers().getFirst(HttpHeaders.AUTHORIZATION));
                    ClientResponse response = ClientResponse.create(HttpStatus.OK)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body("{\"object\":\"list\",\"data\":[{\"embedding\":[0.25],\"index\":0,\"object\":\"embedding\"}],\"model\":\"test-model\",\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":0,\"total_tokens\":1}}")
                            .build();
                    return Mono.just(response);
                })
                .build();
        SiliconFlowEmbeddingClient client = new SiliconFlowEmbeddingClient(
                webClient,
                new ObjectMapper(),
                "https://example.invalid/v1/embeddings",
                "test-only-credential",
                "test-model");

        StepVerifier.create(client.getEmbedding("test note"))
                .assertNext(response -> {
                    assertEquals("test-model", response.model());
                    assertEquals(0.25f, response.data().getFirst().embedding().getFirst());
                })
                .verifyComplete();

        assertTrue(authorization.get().startsWith("Bearer "));
        assertEquals("Bearer test-only-credential", authorization.get());
    }
}
