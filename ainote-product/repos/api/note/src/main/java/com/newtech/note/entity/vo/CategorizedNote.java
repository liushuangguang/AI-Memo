package com.newtech.note.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "归类笔记")
public class CategorizedNote {
    @Schema(description = "对应的note_analysis表里系统备忘录的id")
    private long noteAnalysisId;

    @Schema(description = "note文本信息")
    private String noteText;

    @Schema(description = "note类型", allowableValues = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20"})
    private Integer noteType;
}
