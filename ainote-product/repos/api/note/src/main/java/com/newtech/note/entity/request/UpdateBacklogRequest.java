package com.newtech.note.entity.request;


import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBacklogRequest {
    @Schema(description = "待办id")
    private String id;

    @Schema(description = "待办事项内容", nullable = true)
    private String content;

    @Schema(description = "待办事项描述", nullable = true)
    private String description;

    @Schema(description = "待办时间", nullable = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledAt;

    @Schema(description = "是否完成, false表示未完成, true表示已完成", nullable = true)
    private Boolean isDone;

    @Schema(description = "是否需要通知,由用户设置，false表示不需要通知, true表示需要通知", nullable = true)
    private Boolean isNeedNotify;
}
