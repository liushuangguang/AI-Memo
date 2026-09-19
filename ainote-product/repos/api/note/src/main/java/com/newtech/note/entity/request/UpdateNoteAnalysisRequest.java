package com.newtech.note.entity.request;


import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNoteAnalysisRequest {
    @Schema(description = "用户编辑的笔记标题", nullable = true)
    private String title;

    @Schema(description = "用户编辑的笔记文本", nullable = true)
    private String rawNote;

    @Schema(description = "note分类", nullable = true)
    private Integer noteType;

    @Schema(description = "AI语音内容", nullable = true)
    private JsonNode talkSnapshot;
}
