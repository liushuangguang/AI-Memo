package com.newtech.note.entity.dto;

import com.newtech.note.common.UserLevelEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "userInfo")
@Schema(description = "用户信息表")
public class UserInfoDetail {


    @Schema(description = "可用积分")
    private Double points;

    @Schema(description = "积分变化历史")
    private List<UserPoints> pointsHistory;


    @Schema(description = "用户等级")
    private UserLevelEnum level;
}

