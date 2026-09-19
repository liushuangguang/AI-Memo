package com.newtech.note.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SiliconFlowEmbeddingClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String apiKey;
    private final String model;

    public SiliconFlowEmbeddingClient(WebClient webClient,
                                      ObjectMapper objectMapper,
                                      @Value("${siliconflow.base-url:https://api.siliconflow.cn/v1/embeddings}") String apiUrl,
                                      @Value("${siliconflow.api-key:}") String apiKey,
                                      @Value("${siliconflow.model:BAAI/bge-large-zh-v1.5}") String model) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    public Mono<EmbeddingResponse> getEmbedding(String note) {
        if (!StringUtils.hasText(apiKey)) {
            return Mono.error(new IllegalStateException(
                    "SiliconFlow API key is not configured; set SILICONFLOW_API_KEY"));
        }

        // 创建 payload Map 对象
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("input", note);
        payload.put("encoding_format", "float");
        // 将 Map 转换为 JSON 字符串
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert embedding payload to JSON string");
        }
        return webClient.post()
                .uri(apiUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(jsonPayload)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> {
                    try {
                        EmbeddingResponse embeddingResponse = objectMapper.readValue(response, EmbeddingResponse.class);
                        return Mono.just((embeddingResponse));
                    } catch (IOException e) {
                        return Mono.error(new RuntimeException("Failed to parse embedding response"));
                    }
                });
    }

    public record EmbeddingResponse(
            String object,
            List<Data> data,
            String model,
            Usage usage
    ) {
        public record Data(
                List<Float> embedding,
                int index,
                String object
        ) {
        }

        public record Usage(
                int prompt_tokens,
                int completion_tokens,
                int total_tokens
        ) {
        }
    }
}
