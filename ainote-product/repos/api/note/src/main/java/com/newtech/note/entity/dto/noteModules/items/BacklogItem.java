package com.newtech.note.entity.dto.noteModules.items;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.newtech.note.common.enumeration.NoteItemType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@Schema(description = "待办元素")
@BsonDiscriminator(value = "NOTE_MODULE_ITEM.BACKLOG")
public class BacklogItem implements NoteModuleItem {
    @Schema(description = "item id")
    private String itemId;

    @Schema(description = "item类型")
    private NoteItemType noteItemType = NoteItemType.BACKLOG;

    @Schema(description = "待办内容")
    private String content;

    @Schema(description = "待办描述", nullable = true)
    private String description;

    @Schema(description = "待办时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledAt;

    @Schema(description = "是否删除, false表示未删除, true表示已删除", allowableValues = {"true", "false"}, defaultValue = "false")
    private boolean deleted;

    @Schema(description = "是否添加删除线, false表示未添加, true表示已添加", allowableValues = {"true", "false"}, defaultValue = "false")
    private boolean strikethrough;

    @Schema(description = "是否需要通知,由用户设置，false表示不需要通知, true表示需要通知")
    private boolean needNotify;

    @Schema(description = "是否完成")
    private boolean done;

    @Schema(description = "分词结果", nullable = true)
    @JsonIgnore
    private String segmentation;

}
