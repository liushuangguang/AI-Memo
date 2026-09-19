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
@Schema(description = "分析笔记请求参数")
public class AnalyzeNoteRequestV2 {
    @Schema(description = "note_analysis_records集合的id")
    private String recordId;
}
