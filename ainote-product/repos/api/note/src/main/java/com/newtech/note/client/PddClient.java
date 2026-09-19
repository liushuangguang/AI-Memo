package com.newtech.note.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtech.note.client.entity.PddErrorResponse;
import com.newtech.note.client.entity.PddGoodsPromotionUrlGenerateResponse;
import com.newtech.note.client.entity.PddGoodsSearchResponse;
import io.vavr.control.Either;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author jiajun
 * @date 2021/09/16
 * @description: <a href="https://open.pinduoduo.com/application/document/api?id=pdd.ddk.goods.search">PddDdkGoodsSearch</a>
 */
@Component
public class PddClient {
    private static final String DEFAULT_BASE_URL = "https://gw-api.pinduoduo.com/api/router";
    private static volatile PddClient instance;

    private final WebClient webClient;
    private final Map<String, String> commonParams;
    private final String secretKey;
    private final ObjectMapper objectMapper;

    public PddClient(ObjectMapper objectMapper,
                     @Value("${pdd.base-url:https://gw-api.pinduoduo.com/api/router}") String baseUrl,
                     @Value("${pdd.client-id:}") String clientId,
                     @Value("${pdd.pid:}") String pid,
                     @Value("${pdd.secret-key:}") String secretKey) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.commonParams = Map.of(
                "data_type", "JSON",
                "client_id", clientId,
                "pid", pid
        );
        this.secretKey = secretKey;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void registerSpringManagedInstance() {
        instance = this;
    }

    public static synchronized PddClient getInstance() {
        if (instance == null) {
            instance = new PddClient(
                    new ObjectMapper(),
                    configuredValue("pdd.base-url", "PDD_BASE_URL", DEFAULT_BASE_URL),
                    configuredValue("pdd.client-id", "PDD_CLIENT_ID", ""),
                    configuredValue("pdd.pid", "PDD_PID", ""),
                    configuredValue("pdd.secret-key", "PDD_SECRET_KEY", ""));
        }
        return instance;
    }

    private static String configuredValue(String systemProperty,
                                          String environmentVariable,
                                          String defaultValue) {
        String value = System.getProperty(systemProperty);
        if (!StringUtils.hasText(value)) {
            value = System.getenv(environmentVariable);
        }
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private void requireConfiguration() {
        if (!StringUtils.hasText(commonParams.get("client_id"))) {
            throw new IllegalStateException(
                    "PDD client ID is not configured; set PDD_CLIENT_ID");
        }
        if (!StringUtils.hasText(commonParams.get("pid"))) {
            throw new IllegalStateException(
                    "PDD promotion ID is not configured; set PDD_PID");
        }
        if (!StringUtils.hasText(secretKey)) {
            throw new IllegalStateException(
                    "PDD secret key is not configured; set PDD_SECRET_KEY");
        }
    }

    /**
     * <a href="https://open.pinduoduo.com/application/document/api?id=pdd.ddk.goods.search">拼多多商品搜索</a>
     *
     * @param keyword 搜索关键字
     * @return 商品搜索结果
     */
    public Mono<Either<PddErrorResponse, PddGoodsSearchResponse>> searchGoods(String keyword) {
        return Mono.defer(() -> {
            requireConfiguration();
            Map<String, String> params = new HashMap<>(commonParams);
            params.put("type", "pdd.ddk.goods.search");
            params.put("page_size", "10");
            params.put("sort_type", "2");
            params.put("keyword", keyword);
            params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
            params.put("sign", generateMD5Signature(params, secretKey));

            return webClient.post()
                    .bodyValue(params)
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(responseBody -> {
                        try {
                            JsonNode jsonNode = objectMapper.readTree(responseBody);
                            if (jsonNode.has("error_code")) {
                                PddErrorResponse errorResponse =
                                        objectMapper.treeToValue(jsonNode, PddErrorResponse.class);
                                return Mono.just(Either.<PddErrorResponse, PddGoodsSearchResponse>left(errorResponse));
                            }
                            PddGoodsSearchResponse goodsSearchResponse =
                                    objectMapper.treeToValue(jsonNode, PddGoodsSearchResponse.class);
                            return Mono.just(Either.<PddErrorResponse, PddGoodsSearchResponse>right(goodsSearchResponse));
                        } catch (Exception ignored) {
                            return Mono.error(new IllegalStateException("Unable to parse PDD response"));
                        }
                    });
        });
    }

    public static String generateMD5Signature(Map<String, String> params, String secretKey) {
        TreeMap<String, String> sortedParams = new TreeMap<>(params);
        StringBuilder baseString = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            baseString.append(entry.getKey()).append(entry.getValue());
        }

        String stringToSign = secretKey + baseString + secretKey;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(stringToSign.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 signature algorithm is unavailable", e);
        }
    }

    public Mono<Either<PddErrorResponse, PddGoodsPromotionUrlGenerateResponse>> generateGoodsUrl(
            String goodsSign) {
        return Mono.defer(() -> {
            requireConfiguration();
            Map<String, String> params = new HashMap<>(commonParams);
            params.put("type", "pdd.ddk.goods.promotion.url.generate");
            params.put("goods_sign", goodsSign);
            params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
            params.put("sign", generateMD5Signature(params, secretKey));

            return webClient.post()
                    .bodyValue(params)
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(responseBody -> {
                        try {
                            JsonNode jsonNode = objectMapper.readTree(responseBody);
                            if (jsonNode.has("error_code")) {
                                PddErrorResponse errorResponse =
                                        objectMapper.treeToValue(jsonNode, PddErrorResponse.class);
                                return Mono.just(Either.<PddErrorResponse, PddGoodsPromotionUrlGenerateResponse>left(
                                        errorResponse));
                            }
                            PddGoodsPromotionUrlGenerateResponse response =
                                    objectMapper.treeToValue(
                                            jsonNode, PddGoodsPromotionUrlGenerateResponse.class);
                            return Mono.just(Either.<PddErrorResponse, PddGoodsPromotionUrlGenerateResponse>right(
                                    response));
                        } catch (Exception ignored) {
                            return Mono.error(new IllegalStateException("Unable to parse PDD response"));
                        }
                    });
        });
    }
}
