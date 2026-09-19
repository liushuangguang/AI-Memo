package com.newtech.note.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("note_analysis")
@Schema(description = "笔记")
public class NoteAnalysis {

    @Id
    private long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "note分类，0:未分类")
    private int noteType;
    @Schema(description = "笔记图片", nullable = true)
    private String imageUrl;
    @Schema(description = "设备ID")
    //将来将其改成userId
    private String deviceId;
    @Schema(description = "原始笔记，尚未进行AI分析的笔记")
    private String rawNote;
    @Schema(description = "经过AI分析后的笔记")
    private String noteAnalysisContent;
    @Schema(description = "分词后的笔记内容，用于搜索")
    private String noteContentText;
    @Schema(description = "用于进行全文检索的标签，不用于展示", nullable = true)
    private String hashTags;
    @Schema(description = "和hashTags一一对应，用于展示，所有的标签空格分隔")
    private String tags;

    @Schema(description = "AI 对话内容")
    private String talkSnapshot;

    @Transient
    @Schema(description = "tags列表")
    private List<String> tagList;
    @Transient
    @Schema(description = "搜索时命中的tag列表")
    private List<String> hitTags;

    @Schema(description = "创建时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    @Schema(description = "是否删除, false表示未删除, true表示已删除", allowableValues = {"true", "false"}, defaultValue = "false")
    private boolean isDeleted;
    @Schema(description = "删除时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deletedAt;
    @Schema(description = "维度", allowableValues = "0：用户维度， 1： 系统维度")
    private int dimension;
    @Transient
    @Schema(description = "当前note的所有历史版本", nullable = true)
    private List<NoteAnalysisHistory> noteAnalysisHistories;
}
