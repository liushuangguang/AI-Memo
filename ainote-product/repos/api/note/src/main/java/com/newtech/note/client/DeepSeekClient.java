package com.newtech.note.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible chat-completions client (historical class name retained).
 *
 * <p>The API key is injected from configuration only. Request content,
 * responses and authorization headers are intentionally never logged.</p>
 */
@Component
public class DeepSeekClient {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final Duration requestTimeout;

    public DeepSeekClient(WebClient webClient,
                          ObjectMapper objectMapper,
                          @Value("${ai.chat.base-url:${deepseek.base-url:https://api.deepseek.com}}") String baseUrl,
                          @Value("${ai.chat.api-key:${deepseek.api-key:}}") String apiKey,
                          @Value("${ai.chat.model:${deepseek.model:deepseek-chat}}") String model,
                          @Value("${deepseek.timeout-seconds:90}") long timeoutSeconds) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.baseUrl = removeTrailingSlash(baseUrl);
        this.apiKey = StringUtils.trimToEmpty(apiKey);
        this.model = StringUtils.defaultIfBlank(model, "deepseek-chat").trim();
        this.requestTimeout = Duration.ofSeconds(timeoutSeconds);
    }

    public Mono<DeepSeekCompletion> completeJsonWithUsage(String systemPrompt, String userPrompt) {
        return complete(systemPrompt, userPrompt, true);
    }

    public Mono<DeepSeekCompletion> completeTextWithUsage(String systemPrompt, String userPrompt) {
        return complete(systemPrompt, userPrompt, false);
    }

    private Mono<DeepSeekCompletion> complete(String systemPrompt,
                                              String userPrompt,
                                              boolean jsonResponse) {
        return Mono.defer(() -> {
            if (StringUtils.isBlank(apiKey)) {
                return Mono.error(DeepSeekProviderException.notConfigured());
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", StringUtils.defaultString(systemPrompt)),
                    Map.of("role", "user", "content", StringUtils.defaultString(userPrompt))));
            body.put("temperature", 0.2);
            body.put("stream", false);
            if (jsonResponse) {
                body.put("response_format", Map.of("type", "json_object"));
            }

            return webClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchangeToMono(response -> {
                        if (response.statusCode().isError()) {
                            int status = response.statusCode().value();
                            return response.releaseBody()
                                    .then(Mono.error(DeepSeekProviderException.httpFailure(status)));
                        }
                        return response.bodyToMono(JsonNode.class);
                    })
                    .timeout(requestTimeout)
                    .onErrorMap(this::mapProviderFailure)
                    .flatMap(this::extractContent);
        });
    }

    private Mono<DeepSeekCompletion> extractContent(JsonNode response) {
        JsonNode content = response == null
                ? null
                : response.path("choices").path(0).path("message").path("content");
        if (content == null || !content.isTextual() || StringUtils.isBlank(content.asText())) {
            return Mono.error(DeepSeekProviderException.invalidResponse());
        }
        JsonNode totalTokens = response.path("usage").path("total_tokens");
        if (!totalTokens.canConvertToLong() || totalTokens.asLong() <= 0) {
            return Mono.error(DeepSeekProviderException.invalidUsage());
        }
        String rawContent = content.asText();
        return Mono.just(new DeepSeekCompletion(
                AiResponseSanitizer.stripMarkdownFence(rawContent), rawContent, totalTokens.asLong()));
    }

    private Throwable mapProviderFailure(Throwable failure) {
        if (failure instanceof DeepSeekProviderException) {
            return failure;
        }
        return DeepSeekProviderException.transportFailure(failure);
    }

    private static String removeTrailingSlash(String value) {
        String normalized = StringUtils.defaultIfBlank(value, "https://api.deepseek.com").trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
