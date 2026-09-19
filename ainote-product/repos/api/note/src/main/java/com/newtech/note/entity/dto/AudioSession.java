package com.newtech.note.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("audio_session")
@Schema(description = "音频会话")
public class AudioSession {

    @Id
    @Schema(description = "会话Id")
    private Long sessionId;

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "发送内容")
    private String messageContent;

    @Schema(description = "回复内容")
    private String response;


    @Schema(description = "创建时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @Schema(description = "是否删除, false表示未删除, true表示已删除", allowableValues = {"true", "false"}, defaultValue = "false")
    private boolean isDeleted;

    @Schema(description = "删除时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deletedAt;
}
