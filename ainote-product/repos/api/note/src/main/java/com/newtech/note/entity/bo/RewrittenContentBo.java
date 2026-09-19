package com.newtech.note.entity.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;


@Data
@Schema(description = "复写内容, 包含待办列表")
public class RewrittenContentBo {
    @Schema(description = "被复写的内容")
    private String rewrittenContent;
    @Schema(description = "待办列表")
    private List<TodoBo> todos;
}
