package com.newtech.note.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtech.note.client.entity.bailian.BailianResponse;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Component
public class BailianClient {
    private static final Logger logger = LogManager.getLogger(BailianClient.class);
    private static volatile BailianClient instance;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public BailianClient(WebClient webClient,
                         ObjectMapper objectMapper,
                         @Value("${bailian.api-key:}") String apiKey) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    @PostConstruct
    void registerSpringManagedInstance() {
        instance = this;
    }

    public static synchronized BailianClient getInstance() {
        if (instance == null) {
            WebClient webClient = WebClient.builder()
                    .build();
            instance = new BailianClient(webClient, new ObjectMapper(), configuredApiKey());
        }
        return instance;
    }

    private static String configuredApiKey() {
        String configuredValue = System.getProperty("bailian.api-key");
        if (StringUtils.hasText(configuredValue)) {
            return configuredValue;
        }
        return System.getenv("BAILIAN_API_KEY");
    }

    /**
     * 构建请求体
     *
     * @param params 请求参数
     * @return 请求体
     */
    private String buildRequestBody(Map<String, Object> params) {
        String body;
        try {
            body = objectMapper.writeValueAsString(Map.of("input", Map.of("prompt", "hello", "biz_params", params)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return body;
    }

    /**
     * 调用bailian app api 非流式
     *
     * @param params 参数
     * @return bailian api返回的结果
     */
    public Mono<String> callAppFiltered(String appid, Map<String, Object> params) {
        if (!StringUtils.hasText(apiKey)) {
            return Mono.error(new IllegalStateException(
                    "Bailian API key is not configured; set BAILIAN_API_KEY"));
        }

        String body = buildRequestBody(params);
        logger.info("Calling Bailian app, appid: {}", appid);
        return webClient.post()
                .uri("https://dashscope.aliyuncs.com/api/v1/apps/" + appid + "/completion")
                // .accept(MediaType.TEXT_EVENT_STREAM)
                .header("Authorization",
                        "Bearer " + apiKey.trim())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(rsp -> {
                    try {
                        BailianResponse bailianResponse = objectMapper.readValue(rsp, BailianResponse.class);
                        return Mono.just(bailianResponse.output().text());
                    } catch (JsonProcessingException e) {
                        logger.error("Unable to parse Bailian response, appid: {}", appid);
                        return Mono.error(e);
                    }

                })
                .retryWhen(Retry.fixedDelay(10, Duration.ofMillis(1000))
                        .filter(throwable -> throwable instanceof RuntimeException && throwable.getMessage().contains("retry needed"))
                );
    }


}
