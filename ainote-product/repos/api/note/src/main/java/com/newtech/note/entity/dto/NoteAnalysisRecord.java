package com.newtech.note.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.newtech.note.entity.bo.OrganizedNoteBo;
import com.newtech.note.entity.bo.ProductBo;
import com.newtech.note.entity.dto.noteRelatedInfo.CategorizedNote;
import com.newtech.note.entity.dto.noteRelatedInfo.RelatedLink;
import com.newtech.note.entity.dto.noteRelatedInfo.RelatedTitle;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@Document("note_analysis_records")
@Schema(description = "笔记分析记录")
public class NoteAnalysisRecord {
    @Id
    private String id;
    @Schema(description = "备忘录的id")
    private String noteId;
    @Schema(description = "原始笔记，指ai分析之前的笔记")
    private String rawNote;
    @Schema(description = "Stable digest of the note snapshot")
    private String snapshotHash;
    @Schema(description = "版本号")
    private int version;
    @Schema(description = "总体审查，对备忘录是否有正确的语义进行审核评价", nullable = true)
    private String overallReview;
    @Schema(description = "正文整理后的笔记", nullable = true)
    private OrganizedNoteBo organizedNote;
    @Schema(description = "AI建议", nullable = true)
    private String aiSuggestion;
    @Schema(description = "相关信息", nullable = true)
    private List<RelatedTitle> relatedTitle;

    @Schema(description = "相关链接", nullable = true)
    private List<RelatedLink> relatedLink;

    @Schema(description = "归类笔记", nullable = true)
    public List<CategorizedNote> categorizedNotes;

    @Schema(description = "推荐商品列表", nullable = true)
    private List<ProductBo> recommendedProducts;

    @Schema(description = "AI配图", nullable = true)
    public String aiIllustration;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @Schema(description = "相关备忘录", nullable = true)
    public List<String> relatedNoteIds;
}
