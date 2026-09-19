package com.newtech.note.client.entity.bailian;

import com.fasterxml.jackson.databind.JsonNode;

public record BailianResponse(Output output, String request_id, JsonNode usage) {

    public record Output(String finish_reason, String session_id, String text) {
    }
}
