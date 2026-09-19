package com.newtech.note.entity.request;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.newtech.note.entity.dto.noteModules.NoteModule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateNoteRequest {
    @Schema(description = "用户编辑的笔记title", nullable = true)
    private String title;
    @Schema(description = "用户编辑的笔记文本")
    private String content;
    @Schema(description = "维度", nullable = true, defaultValue = "0")
    private Integer dimension = 0;
    @Schema(description = "note分类", nullable = true)
    private Integer noteType;
    @Schema(description = "模块列表，当前只有在创建系统笔记，也就是在笔记归类时才需要传入", nullable = true)
    @JsonIgnore
    private List<NoteModule> moduleList;
}