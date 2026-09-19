package com.newtech.note.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.newtech.note.common.enumeration.BacklogNotifyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "backlogs")
@Schema(description = "待办")
public class Backlog {
    @Id
    @Schema(description = "待办id")
    private String id;
    @Schema(description = "设备ID")
    private String deviceId;
    /*@Schema(description = "待办标题")
    private String title;*/
    @Schema(description = "待办内容")
    private String content;

    @Schema(description = "待办描述", nullable = true)
    private String description;

    @Schema(description = "待办时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledAt;
    @Schema(description = "创建时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    @Schema(description = "是否删除, false表示未删除, true表示已删除", allowableValues = {"true", "false"}, defaultValue = "false")
    private boolean deleted;
    @Schema(description = "删除时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deletedAt;
    @Schema(description = "是否完成")
    private boolean done;
    @Schema(description = "是否添加删除线, false表示未添加, true表示已添加", allowableValues = {"true", "false"}, defaultValue = "false")
    private boolean strikethrough;
    @Schema(description = "通知状态， 默认为未通知undo， 通知成功succeed，通知失败failed，通知中in progress，删除后为closed")
    private BacklogNotifyStatus notifyStatus;
    @Schema(description = "来源")
    private BacklogFrom backlogFrom;
    @Schema(description = "是否需要通知,由用户设置，false表示不需要通知, true表示需要通知")
    private boolean needNotify;
    @Schema(description = "分词结果")
    @JsonIgnore
    private String segmentation;

    public boolean shouldNotify() {
        if (!isNeedNotify()) {
            return false;
        }
        return notifyStatus == BacklogNotifyStatus.UNDO && scheduledAt.isBefore(LocalDateTime.now());
    }
}
