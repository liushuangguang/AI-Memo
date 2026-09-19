package com.newtech.note.entity.request.noteModule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "删除笔记模块请求参数")
public class DeleteNoteModuleItemRequest {
    @Schema(description = "笔记id")
    private String id;
    @Schema(description = "笔记模块id")
    private String moduleId;
    @Schema(description = "笔记模块元素id")
    private String itemId;
}
