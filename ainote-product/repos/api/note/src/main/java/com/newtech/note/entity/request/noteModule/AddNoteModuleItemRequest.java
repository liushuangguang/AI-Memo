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
public class AddNoteModuleItemRequest {
    @Schema(description = "笔记id")
    private String id;
    @Schema(description = "笔记模块id")
    private String moduleId;
    @Schema(description = "笔记模块元素")
    private CreateNoteModuleItemPayload item;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "noteModuleType")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = CreateBacklogItemRequest.class, name = "BACKLOG"),
            @JsonSubTypes.Type(value = CreateAIRecommendationItemRequest.class, name = "AI_RECOMMENDATION"),
            @JsonSubTypes.Type(value = CreateQAItemRequest.class, name = "QUESTION_ANSWER"),
            @JsonSubTypes.Type(value = CreateKeyValueItemRequest.class, name = "KEY_VALUE_PAIR")
    })
    @Schema(description = "笔记模块接口，用于实现不同的笔记模块类型")
    public interface CreateNoteModuleItemPayload {
        @Schema(description = "模块透传类型")
        NoteModuleType getNoteModuleType();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "创建AI推荐模块请求参数")
    public static class CreateAIRecommendationItemRequest implements CreateNoteModuleItemPayload {
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
    public static class CreateBacklogItemRequest implements CreateNoteModuleItemPayload {
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
        @Schema(description = "代办元素类型", defaultValue = "BACKLOG", allowableValues = {"BACKLOG"})
        public NoteModuleType getNoteModuleType() {
            return NoteModuleType.BACKLOG;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "创建问答模块请求参数")
    public static class CreateQAItemRequest implements CreateNoteModuleItemPayload {
        @Schema(description = "问题")
        private String question;

        @Schema(description = "答案")
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
    public static class CreateKeyValueItemRequest implements CreateNoteModuleItemPayload {
        @Schema(description = "键")
        private String key;

        @Schema(description = "值")
        private String value;

        @Override
        @Schema(description = "模块类型", defaultValue = "KEY_VALUE_PAIR", allowableValues = {"KEY_VALUE_PAIR"})
        public NoteModuleType getNoteModuleType() {
            return NoteModuleType.KEY_VALUE_PAIR;
        }
    }

}
