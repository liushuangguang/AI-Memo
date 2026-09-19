package com.newtech.note.entity.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TodoBo {
    @Schema(description = "待办内容")
    private String content;

    @Schema(description = "待办时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledAt;

    public String toString() {
        return "待办内容：" + content + ", 待办时间：" + scheduledAt;
    }
}
