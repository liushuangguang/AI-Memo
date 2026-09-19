package com.newtech.note.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "猜你想看")
public class RelationalInformationVo {

    @Schema(description = "直接相关信息")
    private String relatedInfo;

    @Schema(description = "相关领域信息")
    private String fieldInfo;
}
