package com.newtech.note.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.newtech.note.entity.dto.noteLogs.NoteOperationLog;
import com.newtech.note.entity.dto.noteModules.NoteModule;
import com.newtech.note.entity.dto.noteModules.TextDataModule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notes")
@Schema(description = "笔记")
public class Note {
    @Id
    private String id;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "note分类，0:未分类")
    private int noteType;
    @Schema(description = "笔记图片", nullable = true)
    private String imageUrl;
    @JsonIgnore
    private String rawOcrText;
    @JsonIgnore
    private String captureRequestId;
    @JsonIgnore
    private String mergeOperationId;
    @Schema(description = "设备ID")
    //将来将其改成userId
    private String deviceId;

    @Schema(description = "备忘录主题ID", nullable = true)
    private List<String> noteThemeIds;

    private List<NoteModule> modules;
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
    @Schema(description = "维度", allowableValues = "0：用户维度， 1： 系统维度")
    private int dimension;
    @Schema(description = "笔记操作日志")
    private List<NoteOperationLog> operationLogs;

    @JsonIgnore
    private Map<String, String> voiceSummarySaveDigests;

    @Schema(description = "笔记内容，从module list中获取，获取第一个TextDataModule的content")
    @Transient
    private String content;

    public String getContent() {
        if (CollectionUtils.isEmpty(modules)) {
            return null;
        }
        return modules.stream()
                .filter(module -> module instanceof TextDataModule)
                .map(TextDataModule.class::cast)
                .map(TextDataModule::getCleanedContent)
                .findFirst()
                .orElse(null);
    }

}
