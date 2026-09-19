package com.newtech.note.entity.request.noteModule;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.newtech.note.common.enumeration.NoteItemType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "添加笔记模块请求参数")
public class ReplaceTextModuleRequest {
    @Schema(description = "笔记id")
    private String id;
    @Schema(description = "整理后的标题")
    private String title;

    @Schema(description = "整理后的正文")
    private String organizedNote;
    @Schema(description = "代办事项列表", nullable = true)
    private List<NoteModuleItemPayload> noteModuleItems;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "noteItemType")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = BacklogItemRequest.class, name = "BACKLOG"),
            @JsonSubTypes.Type(value = RecommendationItemRequest.class, name = "AI_RECOMMENDATION"),
    })
    @Schema(description = "笔记模块接口，用于实现不同的笔记元素类型")
    public interface NoteModuleItemPayload {
        @Schema(description = "模块透传类型")
        NoteItemType getNoteItemType();
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @Schema(description = "待办元素")
    public static class BacklogItemRequest implements NoteModuleItemPayload {
        @Schema(description = "待办内容")
        private String content;
        @Schema(description = "待办描述", nullable = true)
        private String description;
        @Schema(description = "待办时间")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime scheduledAt;
        @Schema(description = "是否需要通知,由用户设置，false表示不需要通知, true表示需要通知")
        private boolean needNotify;

        @Override
        public NoteItemType getNoteItemType() {
            return NoteItemType.BACKLOG;
        }
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @Schema(description = "推荐元素")
    public static class RecommendationItemRequest implements NoteModuleItemPayload {
        @Schema(description = "推荐内容")
        private String suggestion;
        @Schema(description = "推荐emoji", nullable = true)
        private String emoji;

        @Override
        public NoteItemType getNoteItemType() {
            return NoteItemType.AI_RECOMMENDATION;
        }
    }
}
