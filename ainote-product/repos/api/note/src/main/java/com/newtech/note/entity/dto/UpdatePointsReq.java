package com.newtech.note.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePointsReq {

    @Schema(description = "积分变化行为：add增加，consumer消耗")
    private PointsChangeEnum changeMethod;

    @Schema(description = "变化的积分")
    private Double changePoint;

    @Schema(description = "积分过期时间")
    private PointsDataEnum days;
}

