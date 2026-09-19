package com.newtech.note.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "userPoints")
@Schema(description = "用户积分token记录表")
public class UserPoints {

    @Id
    @Schema(description = "唯一标识符，用户ID和时间戳组合")
    private String id;

    @Schema(description = "用户id")
    private Long uid;

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "用户变化的积分")
    private Double changePoint;

    @Schema(description = "用户变化的token")
    private Long token;

    @Schema(description = "积分变化")
    private PointsChangeEnum changeMethod;

    @Schema(description = "创建时间")
    private Date createdAt;

    @Schema(description = "过期时间")
    private Date expireAt;
}

