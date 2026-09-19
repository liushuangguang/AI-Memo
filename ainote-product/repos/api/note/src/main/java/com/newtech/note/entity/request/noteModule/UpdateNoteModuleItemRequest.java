package com.newtech.note.entity.request.noteModule;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.newtech.note.common.enumeration.NoteModuleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "添加笔记模块元素请求参数")
public class UpdateNoteModuleItemRequest {
    @Schema(description = "笔记id")
    private String id;
    @Schema(description = "笔记模块id")
    private String moduleId;
    @Schema(description = "笔记模块元素id")
    private String itemId;
    @Schema(description = "笔记模块元素")
    private UpdateNoteModuleItemPayload item;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "noteModuleType")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = UpdateBacklogItemRequest.class, name = "BACKLOG"),
            @JsonSubTypes.Type(value = UpdateAIRecommendationItemRequest.class, name = "AI_RECOMMENDATION"),
            @JsonSubTypes.Type(value = UpdateQAItemRequest.class, name = "QUESTION_ANSWER"),
            @JsonSubTypes.Type(value = UpdateKeyValueItemRequest.class, name = "KEY_VALUE_PAIR")
    })
    @Schema(description = "更新笔记模块元素接口，用于实现不同的笔记模块元素类型")
    public interface UpdateNoteModuleItemPayload {
        @Schema(description = "模块透传类型")
        NoteModuleType getNoteModuleType();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "创建AI推荐模块请求参数")
    public static class UpdateAIRecommendationItemRequest implements UpdateNoteModuleItemPayload {
        @Schema(description = "描述")
        private String description;

        @Override
        @Schema(description = "AI推荐元素", defaultValue = "AI_RECOMMENDATION", allowableValues = {"AI_RECOMMENDATION"})
        public NoteModuleType getNoteModuleType() {
            return NoteModuleType.AI_RECOMMENDATION;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "创建待办模块请求参数")
    public static class UpdateBacklogItemRequest implements UpdateNoteModuleItemPayload {
        @Schema(description = "待办内容", nullable = true)
        private String content;
        @Schema(description = "待办描述", nullable = true)
        private String description;
        @Schema(description = "待办时间", nullable = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime scheduledAt;
        @Schema(description = "是否需要通知，由用户设置，false表示不需要通知, true表示需要通知，null表示不设置", nullable = true, allowableValues = {"true", "false"})
        private Boolean isNeedNotify;
        @Schema(description = "是否完成，由用户设置，false表示未完成，true表示已完成，null表示不设置", nullable = true, allowableValues = {"true", "false"})
        private Boolean isDone;

        @Override
        @Schema(description = "代办元素类型", defaultValue = "BACKLOG", allowableValues = {"BACKLOG"})
        public NoteModuleType getNoteModuleType() {
            return NoteModuleType.BACKLOG;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "创建问答模块请求参数")
    public static class UpdateQAItemRequest implements UpdateNoteModuleItemPayload {
        @Schema(description = "问题", nullable = true)
        private String question;

        @Schema(description = "答案", nullable = true)
        private String answer;

        @Override
        @Schema(description = "模块类型", defaultValue = "QUESTION_ANSWER", allowableValues = {"QUESTION_ANSWER"})
        public NoteModuleType getNoteModuleType() {
            return NoteModuleType.QUESTION_ANSWER;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "创建问答模块请求参数")
    public static class UpdateKeyValueItemRequest implements UpdateNoteModuleItemPayload {
        @Schema(description = "键", nullable = true)
        private String Key;

        @Schema(description = "值", nullable = true)
        private String value;

        @Override
        @Schema(description = "模块类型", defaultValue = "KEY_VALUE_PAIR", allowableValues = {"KEY_VALUE_PAIR"})
        public NoteModuleType getNoteModuleType() {
            return NoteModuleType.KEY_VALUE_PAIR;
        }
    }

}
