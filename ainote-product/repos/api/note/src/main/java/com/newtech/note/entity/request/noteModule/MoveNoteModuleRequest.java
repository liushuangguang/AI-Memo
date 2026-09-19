package com.newtech.note.entity.request.noteModule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "移动笔记模块请求参数")
public class MoveNoteModuleRequest {
    @Schema(description = "笔记id")
    private String id;
    @Schema(description = "需要移动的笔记模块id")
    private String moduleId;
    @Schema(description = "移动到该笔记模块之后, 如果为null, 则移动到第一个")
    private String moveAfter;
}
