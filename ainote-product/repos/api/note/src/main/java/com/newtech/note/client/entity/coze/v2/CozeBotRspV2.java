package com.newtech.note.client.entity.coze.v2;

import java.util.List;

public record CozeBotRspV2(List<CozeBotRspMessageV2> messages, String code, String msg, String conversation_id) {
}
