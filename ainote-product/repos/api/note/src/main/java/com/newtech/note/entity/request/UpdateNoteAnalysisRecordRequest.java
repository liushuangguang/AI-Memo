package com.newtech.note.entity.request;

import com.newtech.note.entity.bo.OrganizedNoteBo;
import com.newtech.note.entity.bo.ProductBo;
import com.newtech.note.entity.dto.noteRelatedInfo.CategorizedNote;
import com.newtech.note.entity.dto.noteRelatedInfo.RelatedLink;
import com.newtech.note.entity.dto.noteRelatedInfo.RelatedTitle;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class UpdateNoteAnalysisRecordRequest {
    @Schema(description = "笔记分析历史id, note_analysis_records集合的ID")
    private String id;
    @Schema(description = "总体审查，对备忘录是否有正确的语义进行审核评价", nullable = true)
    private String overallReview;
    @Schema(description = "正文整理后的笔记", nullable = true)
    private OrganizedNoteBo organizedNote;
    @Schema(description = "AI建议", nullable = true)
    private String aiSuggestion;
    @Schema(description = "相关标题", nullable = true)
    private List<RelatedTitle> relatedTitle;
    @Schema(description = "相关链接", nullable = true)
    private List<RelatedLink> relatedLink;
    @Schema(description = "相关链接", nullable = true)
    private String aiIllustration;
    @Schema(description = "分类后的笔记，经过分类后，当前笔记的某些部分会被归类到系统维度的某个类型的笔记中，成为这些笔记的一个module，并且这些模块都是使用键值对方式进行存储的", nullable = true)
    private List<CategorizedNote> categorizedNote;
    @Schema(description = "推荐商品", nullable = true)
    private List<ProductBo> recommendedProducts;
    @Schema(description = "相关备忘录", nullable = true)
    private List<String> relatedNoteIds;
}
