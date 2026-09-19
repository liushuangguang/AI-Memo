package com.newtech.note.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "辅助内容请求")

public class AssistContentRequest {
    @Schema(description = "note_analysis表的id")
    private long id;
    @Schema(description = "版本号")
    private int version;
    @Schema(description = "协助方式及类型")
    private List<String> assistDirections;
    @Schema(description = "选中的标题", nullable = true)
    private String selectedTitle;
    @Schema(description = "选中的内容", nullable = true)
    private String selectedContent;

    /*@Schema(description = "是否从本地数据库中获取")
    private boolean fromDB;*/
}
