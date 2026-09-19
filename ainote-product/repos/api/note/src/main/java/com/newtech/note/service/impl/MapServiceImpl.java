package com.newtech.note.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtech.note.client.CozeClient;
import com.newtech.note.client.DifyClient;
import com.newtech.note.entity.dto.map.NearByMerchants;
import com.newtech.note.service.MapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j

public class MapServiceImpl implements MapService {

    @Value("${workflow_id.map.near_by_merchants:7431880565655306279}")
    private String workflowId;

    @Value("${workflow_id.map.query_product_keywords:app-YFC0KDRn0kS1LdNVw11h8SgR}")
    private String apiKeyForQueryProductKeywords;

    private final ObjectMapper objectMapper;
    private final DifyClient difyClient;
    private final CozeClient cozeClient;

    public MapServiceImpl(ObjectMapper objectMapper, DifyClient difyClient, CozeClient cozeClient) {
        this.difyClient = difyClient;
        this.objectMapper = objectMapper;
        this.cozeClient = cozeClient;
    }

    @Override
    public Mono<NearByMerchants> searchNearByMerchants(String address, String keyword, ServerHttpRequest req) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("address", address);
        parameters.put("keyword", keyword);

        return cozeClient
                .callCozeWorkflowApiFiltered(workflowId, "", parameters, null, req)
                .flatMap(response -> {
                    try {
                        NearByMerchants merchants = objectMapper.readValue(response, NearByMerchants.class);
                        return Mono.justOrEmpty(merchants);
                    } catch (Exception e) {
                        log.error("Error parsing NearByMerchants response", e);
                        return Mono.empty();
                    }
                });
    }

    @Override
    public Mono<String> queryProductKeywords(String input, ServerHttpRequest req) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("inp", input);

        return difyClient
                .callDifyBlockingWorkflowApiFiltered(apiKeyForQueryProductKeywords, parameters, "res", req)
                .flatMap(response -> {
                    try {
                        JsonNode data = objectMapper.readTree(response);
                        String result = data.path("productKeywords").asText(null);
                        return Mono.justOrEmpty(result);
                    } catch (Exception e) {
                        log.error("Error parsing product keywords response", e);
                        return Mono.empty();
                    }
                });
    }
}
