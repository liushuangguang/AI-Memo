package com.newtech.note.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "更新分类笔记请求")
@Getter
@Setter
@NoArgsConstructor
public class UpdateCategorizedNoteRequest {

    @Schema(description = "categorized_note的id, 用于找到对应的note")
    private long id;

    @Schema(description = "待更新的分类")
    private Integer noteType;

    @Schema(description = "待更新的文本内容")
    private String noteText;
}
