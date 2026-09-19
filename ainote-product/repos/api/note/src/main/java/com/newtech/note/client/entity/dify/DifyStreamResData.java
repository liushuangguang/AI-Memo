package com.newtech.note.client.entity.dify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DifyStreamResData(String text,
                                String status,
                                JsonNode total_tokens) {
}
