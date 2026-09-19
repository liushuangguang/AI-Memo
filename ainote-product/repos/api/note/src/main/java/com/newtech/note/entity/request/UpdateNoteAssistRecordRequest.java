package com.newtech.note.entity.request;

import com.newtech.note.entity.bo.RewrittenContentBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class UpdateNoteAssistRecordRequest {
    @Schema(description = "笔记分析历史id, note_assist_records集合的ID")
    private String id;
    @Schema(description = "总体审查，对备忘录是否有正确的语义进行审核评价", nullable = true)
    private String overallReview;

    @Schema(description = "辅助的方向", nullable = true)
    private List<String> assistDirection;

    @Schema(description = "复写后的内容，和辅助的方向一一对应", nullable = true)
    private RewrittenContentBo rewrittenContent;

    @Schema(description = "选中的内容", nullable = true)
    private String selectedContent;
}
