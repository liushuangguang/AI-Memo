package com.newtech.note.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DeepSeekClientTest {

    @Test
    void callsOpenAiCompatibleEndpointAndNormalizesJsonFence() {
        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        ExchangeFunction exchange = request -> {
            captured.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body("""
                            {"choices":[{"message":{"content":"```json\\n{\\\"result\\\":\\\"1\\\"}\\n```"}}],
                            "usage":{"total_tokens":12}}
                            """)
                    .build());
        };
        DeepSeekClient client = new DeepSeekClient(
                WebClient.builder().exchangeFunction(exchange).build(),
                new ObjectMapper(),
                "https://api.deepseek.com/",
                "test-api-key",
                "deepseek-chat",
                5);

        StepVerifier.create(client.completeJsonWithUsage("system", "user"))
                .assertNext(completion -> {
                    assertThat(completion.content()).isEqualTo("{\"result\":\"1\"}");
                    assertThat(completion.rawContent())
                            .isEqualTo("```json\n{\"result\":\"1\"}\n```");
                    assertThat(completion.totalTokens()).isEqualTo(12);
                })
                .verifyComplete();

        assertThat(captured.get().url().toString())
                .isEqualTo("https://api.deepseek.com/chat/completions");
        assertThat(captured.get().headers().getFirst("Authorization"))
                .isEqualTo("Bearer test-api-key");
    }

    @Test
    void missingKeyFailsBeforeAnyNetworkRequest() {
        AtomicInteger calls = new AtomicInteger();
        ExchangeFunction exchange = request -> {
            calls.incrementAndGet();
            return Mono.error(new AssertionError("network must not be called"));
        };
        DeepSeekClient client = new DeepSeekClient(
                WebClient.builder().exchangeFunction(exchange).build(),
                new ObjectMapper(),
                "https://api.deepseek.com",
                " ",
                "deepseek-chat",
                5);

        StepVerifier.create(client.completeTextWithUsage("system", "user"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(DeepSeekProviderException.class);
                    assertThat(error.getMessage()).contains("DEEPSEEK_API_KEY");
                })
                .verify();
        assertThat(calls).hasValue(0);
    }

    @Test
    void rejectsResponseWithoutUsageSoMobileAccountingCannotBeBypassed() {
        ExchangeFunction exchange = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body("{\"choices\":[{\"message\":{\"content\":\"result\"}}]}")
                .build());
        DeepSeekClient client = new DeepSeekClient(
                WebClient.builder().exchangeFunction(exchange).build(),
                new ObjectMapper(),
                "https://api.deepseek.com",
                "test-api-key",
                "deepseek-chat",
                5);

        StepVerifier.create(client.completeTextWithUsage("system", "user"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(DeepSeekProviderException.class);
                    assertThat(error.getMessage()).contains("token usage");
                })
                .verify();
    }
}
