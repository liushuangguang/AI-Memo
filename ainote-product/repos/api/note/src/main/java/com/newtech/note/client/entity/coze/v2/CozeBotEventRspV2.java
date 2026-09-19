package com.newtech.note.client.entity.coze.v2;

public record CozeBotEventRspV2(String event, CozeBotRspMessageV2 message, boolean is_finish, int index,
                                String conversation_id) {
}
