package com.newtech.note.entity.request;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateNoteAnalysisRequest {
    @Schema(description = "用户编辑的笔记title", nullable = true)
    private String title;
    @Schema(description = "用户编辑的笔记文本")
    private String rawNote;
    @Schema(description = "维度", nullable = true, defaultValue = "0")
    private Integer dimension = 0;
    @Schema(description = "note分类", nullable = true)
    private Integer noteType;
}