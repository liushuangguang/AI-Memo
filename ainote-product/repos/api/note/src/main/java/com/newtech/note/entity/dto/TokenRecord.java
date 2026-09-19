package com.newtech.note.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table("user_token_record")
@Schema(description = "token记录表")
public class TokenRecord {
    @Id
    private String deviceId;
    @Schema(description = "用户拥有的token")
    private Long totalToken;
    @Schema(description = "消耗的token")
    private Long token;
    @Schema(description = "用户id")
    private Long uid;

}
