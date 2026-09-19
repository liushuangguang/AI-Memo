package com.newtech.note.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "分析文本请求参数")
public class AnalyzeTextContentRequest {
    @Schema(description = "note_analysis_records集合的id",nullable = true)
    private String recordId;

    @Schema(description = "待分析的笔记内容, 若为空则默认使用recordId对应的笔记内容", nullable = true)
    private String specificContent;
}
