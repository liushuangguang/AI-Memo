package com.newtech.note.entity.request;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateNoteThemeRequest {
    @Schema(description = "用户编辑的笔记theme", nullable = true)
    private String theme;
    @Schema(description = "用户编辑的笔记theme的描述")
    private String description;
}