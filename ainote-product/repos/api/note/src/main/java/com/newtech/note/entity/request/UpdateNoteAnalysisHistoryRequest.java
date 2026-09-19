package com.newtech.note.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNoteAnalysisHistoryRequest {
    @Schema(description = "笔记分析id, note_analysis表的ID")
    private long noteAnalysisId;

    @Schema(description = "version, 版本号")
    private int version;

    @Schema(description = "分析类型, 1.一键整理，2.内容辅助，3.语音讨论")
    private int analysisType;

    @Schema(description = "rawNoteText, 原始笔记文本")
    private String rawNoteText;

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

    @Schema(description = "当前rawNote相关的链接")
    private String assistedContent;

    @Schema(description = "当前rawNote图片的链接")
    private String imageLink;

    @Schema(description = "猜你想看：相关信息")
    private String relationalRelatedInfo;

    @Schema(description = "猜你想看：相关领域")
    private String relationalFieldInfo;

    @Schema(description = "聊天会话快照记录")
    private String talkSnapshot;

}
