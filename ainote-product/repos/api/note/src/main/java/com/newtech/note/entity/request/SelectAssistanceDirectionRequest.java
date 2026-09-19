package com.newtech.note.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "选中辅助方式请求")
public class SelectAssistanceDirectionRequest {
    @Schema(description = "note_analysis表的id")
    private long id;
}
