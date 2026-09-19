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

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "userInfo")
@Schema(description = "用户信息表")
public class UserInfo {

    @Id
    @Schema(description = "用户id")
    private Long uid;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "用户变化的积分")
    private String nickName;

    @Schema(description = "token")
    private String token;

    @Schema(description = "创建时间")
    private Date createdAt;

    @Schema(description = "更新时间")
    private Date updateAt;

    @Schema(description = "用户等级")
    private UserLevelEnum level;

    public UserInfo(Long uid, String mobile) {
    }
}

