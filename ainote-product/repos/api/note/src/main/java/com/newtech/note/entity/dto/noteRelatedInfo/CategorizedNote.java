package com.newtech.note.entity.dto.noteRelatedInfo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.newtech.note.entity.dto.noteModules.KeyValueModule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Transient;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "分类后的笔记信息")
public class CategorizedNote {
    @JsonIgnore
    @Schema(description = "分类后的笔记的id")
    private String noteId;

    @JsonIgnore
    @Schema(description = "分类后的笔记对应的模块id")
    private String moduleId;

    @Transient
    @Schema(description = "分类后的笔记对应的模块信息")
    private KeyValueModule categorizedNoteModule;
}
