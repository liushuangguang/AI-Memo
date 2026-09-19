package com.newtech.note.entity.dto;

import com.newtech.note.entity.bo.RewrittenContentBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;


@Data
@Schema(description = "复写内容, 包含待办列表")
public class RewrittenContentDto {
    @Schema(description = "被复写的内容")
    private String rewrittenContent;
    @Schema(description = "待办列表")
    private List<TodoDto> todos;

    public RewrittenContentBo transferToBo() {
        RewrittenContentBo bo = new RewrittenContentBo();
        bo.setRewrittenContent(this.getRewrittenContent());
        bo.setTodos(TodoDto.transferListToBoList(this.getTodos()));
        return bo;
    }
}
