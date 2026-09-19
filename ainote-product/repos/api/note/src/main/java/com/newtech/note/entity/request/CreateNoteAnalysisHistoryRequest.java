package com.newtech.note.entity.request;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateNoteAnalysisHistoryRequest {
    @Schema(description = "笔记分析id, note_analysis表的ID")
    private long noteAnalysisId;

   /* @Schema(description = "note_message, 笔记内容")
    private String noteMessage;*/

    @Schema(description = "note_analysis_type, 笔记分析类型",allowableValues = "1:一键整理, 2:内容辅助, 3:语音讨论")
    private int analysisType;
}
