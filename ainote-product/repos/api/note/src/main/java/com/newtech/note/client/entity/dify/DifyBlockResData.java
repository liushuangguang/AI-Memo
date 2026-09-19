package com.newtech.note.client.entity.dify;

import java.sql.Timestamp;
import java.util.Map;

public record DifyBlockResData(String id, String workflow_id, String status, Map<String, String> outputs, String error, Float elapsed_time, Integer total_tokens, Integer total_steps, Timestamp created_at, Timestamp finished_at) {
}
