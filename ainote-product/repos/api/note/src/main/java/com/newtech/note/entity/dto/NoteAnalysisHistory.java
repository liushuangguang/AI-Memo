package com.newtech.note.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.newtech.note.common.enumeration.NoteAnalysisType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table("note_analysis_history")
@Schema(description = "笔记")
public class NoteAnalysisHistory {
    @Id
    private long id;
    @Schema(description = "原始笔记，指ai分析之前的笔记")
    private String rawNote;
    @Schema(description = "note_analysis表的id")
    private long noteAnalysisId;
    @Schema(description = "版本号")
    private int version;
    @Schema(description = "正文整理后的笔记")
    private String organizedNoteText;
    @Schema(description = "当前rawNote相关的笔记")
    private String relatedNotes;
    @Schema(description = "当前rawNote相同类型的笔记")
    private String categorizedNotes;
    @Schema(description = "当前rawNote涉及的商品推荐")
    private String productRecommendations;
    @Schema(description = "当前rawNote相关的链接")
    private String relatedLinks;
    @Schema(description = "当前rawNote图片相关的链接")
    private String imageLink;

    @Schema(description = "猜你想看：相关信息")
    private String relationalRelatedInfo;
    @Schema(description = "猜你想看：相关领域")
    private String relationalFieldInfo;

    @Schema(description = "聊天会话快照记录")
    private String talkSnapshot;

    @Schema(description = "当前rawNote的辅助内容")
    private String assistedContent;
    @Schema(description = "创建时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    @Schema(description = "分析类型；1-整理笔记，2-辅助内容，3-讨论")
    private int analysisType;

    public boolean isEmpty() {
        NoteAnalysisType type = NoteAnalysisType.of(analysisType);
        if (type == null) {
            return true;
        }
        return switch (type) {
            case ORGANIZE_WITH_ONE_CLICK ->
                    StringUtils.isAllBlank(organizedNoteText, relatedNotes, categorizedNotes, productRecommendations, relatedLinks);
            case ASSIST_CONTENT -> StringUtils.isBlank(assistedContent);
            case DISCUSS_BY_VOICE -> true;
        };
    }
}
