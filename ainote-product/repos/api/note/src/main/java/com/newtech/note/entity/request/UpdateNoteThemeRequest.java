package com.newtech.note.entity.request;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "更新笔记请求参数")
public class UpdateNoteThemeRequest {
    @Schema(description = "笔记主题id")
    private String id;

    @Schema(description = "用户编辑的笔记主题", nullable = true)
    private String theme;

    @Schema(description = "用户编辑的主题描述", nullable = true)
    private String description;
}
