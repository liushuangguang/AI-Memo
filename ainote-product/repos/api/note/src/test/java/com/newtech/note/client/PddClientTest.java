package com.newtech.note.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PddClientTest {

    @Test
    void failsBeforeNetworkCallWhenRequiredConfigurationIsMissing() {
        PddClient client = new PddClient(
                new ObjectMapper(),
                "https://example.invalid/api/router",
                "",
                "",
                "");

        StepVerifier.create(client.searchGoods("test"))
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().contains("PDD_CLIENT_ID"))
                .verify();
    }

    @Test
    void signatureIsIndependentOfInputMapOrder() {
        Map<String, String> first = new LinkedHashMap<>();
        first.put("type", "test.operation");
        first.put("client_id", "test-client");
        Map<String, String> second = new LinkedHashMap<>();
        second.put("client_id", "test-client");
        second.put("type", "test.operation");

        String firstSignature = PddClient.generateMD5Signature(first, "test-only-secret");
        String secondSignature = PddClient.generateMD5Signature(second, "test-only-secret");

        assertEquals(firstSignature, secondSignature);
    }
}
