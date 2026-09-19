package com.newtech.note.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "由于没有文本基础，直接把用户最新的会话内容当中基础来进行整理")

public class OrganizeToDoListNoteRequest {

    @Schema(description = "会话Id")
    private long sessionId;

    @Schema(description = "设备Id")
    private String deviceId;

    @Schema(description = "对话内容")
    private String messageContent;

}
