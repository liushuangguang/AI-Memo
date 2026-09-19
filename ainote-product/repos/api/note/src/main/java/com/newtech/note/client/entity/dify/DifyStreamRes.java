package com.newtech.note.client.entity.dify;

public record DifyStreamRes(String event, String workflow_run_id, String task_id,
                            DifyStreamResData data) {
}
