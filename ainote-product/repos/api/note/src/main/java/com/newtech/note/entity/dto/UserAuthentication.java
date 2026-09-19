package com.newtech.note.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("note_analysis")
@Schema(description = "用户信息")
public class UserAuthentication {
    /**
     * 设备id
     */
    private String deviceId;

    /**
     * id
     */
    private Long uid;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 邮箱
     */
    private String email;
}
