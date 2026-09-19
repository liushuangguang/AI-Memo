package com.newtech.note.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(description = "由于没有文本基础，直接把用户最新的会话内容当中基础来进行整理")
public class GetNoteDiscussAudioRequest {

    @Schema(description = "会话Id (对应 note_analysis_history 的 id 字段)")
    private long sessionId;

    @Schema(description = "设备Id")
    private String deviceId;
}
