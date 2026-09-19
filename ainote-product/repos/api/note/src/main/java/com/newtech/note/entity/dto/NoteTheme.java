package com.newtech.note.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "note_themes")
@Schema(description = "笔记主题")
public class NoteTheme {
    @Id
    private String id;
    @Schema(description = "主题")
    private String theme;
    @Schema(description = "主题描述")
    private String description;
    @Schema(description = "最近一次合并生成的笔记ID", nullable = true)
    private String mergedNoteId;
    @Schema(description = "可追溯的主题合并历史")
    private List<NoteThemeMergeHistory> mergeHistory;
    @Schema(description = "分词结果")
    @JsonIgnore
    private String segmentation;

    @Schema(description = "设备ID")
    //将来将其改成userId
    private String deviceId;

    @Schema(description = "创建时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    @Schema(description = "是否删除, false表示未删除, true表示已删除", allowableValues = {"true", "false"}, defaultValue = "false")
    private boolean deleted;
    @Schema(description = "删除时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deletedAt;
}
