package com.newtech.note.entity.request;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.newtech.note.entity.dto.BacklogFrom;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBacklogRequest {
    @Schema(description = "待办内容")
    private String content;
    @Schema(description = "待办描述", nullable = true)
    private String description;
    @Schema(description = "待办时间, 日期格式为yyyy-MM-dd'T'HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledAt;
    @Schema(description = "来源", nullable = true)
    private BacklogFrom backlogFrom;
}