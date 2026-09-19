package com.newtech.note.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@Schema(description = "自动整理分析笔记请求参数")
public class AutoAnalyzeRequest {
    @Schema(description = "notes集合的id")
    private String noteId;
}
