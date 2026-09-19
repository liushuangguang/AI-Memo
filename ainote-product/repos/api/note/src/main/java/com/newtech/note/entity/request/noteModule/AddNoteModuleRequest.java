package com.newtech.note.entity.request.noteModule;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.newtech.note.common.enumeration.NoteModuleType;
import com.newtech.note.common.enumeration.RecommendationItemType;
import com.newtech.note.entity.dto.noteModules.items.KeyValueItem;
import com.newtech.note.entity.dto.noteModules.items.ProductRecommendationItem;
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
public class AddNoteModuleRequest {
    @Schema(description = "笔记id")
    private String id;
    /*@Schema(description = "笔记模块",
            oneOf = {CreateAIPictureModuleRequest.class,
                    CreateAIRecommendationModuleRequest.class,
                    CreateBacklogModuleRequest.class,
                    CreateQAModuleRequest.class,
                    CreateRelatedNoteModuleRequest.class,
                    CreateTextDataModuleRequest.class}
    )*/
    @Schema(description = "笔记模块")
    private CreateNoteModulePayload module;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "noteModuleType")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = CreateAIPictureModuleRequest.class, name = "AI_PICTURE"),
            @JsonSubTypes.Type(value = CreateAIRecommendationModuleRequest.class, name = "AI_RECOMMENDATION"),
            @JsonSubTypes.Type(value = CreateBacklogModuleRequest.class, name = "BACKLOG"),
            @JsonSubTypes.Type(value = CreateQAModuleRequest.class, name = "QUESTION_ANSWER"),
            @JsonSubTypes.Type(value = CreateRelatedNoteModuleRequest.class, name = "RELATED_NOTE"),
            @JsonSubTypes.Type(value = CreateTextDataModuleRequest.class, name = "TEXT_DATA"),
            @JsonSubTypes.Type(value = CreateScenarioRecordModuleRequest.class, name = "SCENARIO_RECORD"),
            @JsonSubTypes.Type(value = CreateKeyValueModuleRequest.class, name = "KEY_VALUE_PAIR")
    })
    @Schema(description = "笔记模块接口，用于实现不同的笔记模块类型")
    public interface CreateNoteModulePayload {
        @Schema(description = "模块透传类型")
        NoteModuleType getNoteModuleType();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "创建AI图片模块请求参数")
    public static class CreateAIPictureModuleRequest implements CreateNoteModulePayload {
        @Schema(description = "图片地址")
        private String imageUrl;
        @Schema(description = "标题")
        private String title;
        @Schema(description = "描述")
        private String description;

        @Override
        @Schema(description = "模块类型", defaultValue = "AI_PICTURE", allowableValues = {"AI_PICTURE"})
        public NoteModuleType getNoteModuleType() {
            return NoteModuleType.AI_PICTURE;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "创建AI推荐模块请求参数")
    public static class CreateAIRecommendationModuleRequest implements CreateNoteModulePayload {
        @Schema(description = "标题")
        private String title;
        @Schema(description = "推荐元素列表")
        private List<RecommendationItemRequest> recommendationItems;

        @Override
        @Schema(description = "模块类型", defaultValue = "AI_RECOMMENDATION", allowableValues = {"AI_RECOMMENDATION"})
        public NoteModuleType getNoteModuleType() {
            return NoteModuleType.AI_RECOMMENDATION;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "创建待办模块请求参数")
    public static class CreateBacklogModuleRequest implements CreateNoteModulePayload {
        @Schema(description = "名称")
        private String name;
        @Schema(description = "代办事项列表")
        private List<BacklogItemRequest> backlogItems;

        @Override
        @Schema(description = "模块类型", defaultValue = "BACKLOG", allowableValues = {"BACKLOG"})
        public NoteModuleType getNoteModuleType() {
            return NoteModuleType.BACKLOG;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "创建问答模块请求参数")
    public static class CreateQAModuleRequest implements CreateNoteModulePayload {
        @Schema(description = "问答元素列表")
        private List<QAItemRequest> questionAnswerItems;

        @Override
        @Schema(description = "模块类型", defaultValue = "QUESTION_ANSWER", allowableValues = {"QUESTION_ANSWER"})
        public NoteModuleType getNoteModuleType() {
            return NoteModuleType.QUESTION_ANSWER;
        }
    }


    // RelatedNoteModuleRequest 实现类
    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "创建相关笔记模块请求参数")
    public static class CreateRelatedNoteModuleRequest implements CreateNoteModulePayload {
        @Schema(description = "相关笔记id列表")
        private List<String> relatedNoteIds;

        @Override
        @Schema(description = "模块类型", defaultValue = "RELATED_NOTE", allowableValues = {"RELATED_NOTE"})
        public NoteModuleType getNoteModuleType() {
            return NoteModuleType.RELATED_NOTE;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "创建文本数据模块请求参数")
    public static class CreateTextDataModuleRequest implements CreateNoteModulePayload {
        @Schema(description = "标题")
        private String title;
        @Schema(description = "内容")
        private String content;

        @Override
        @Schema(description = "模块类型", defaultValue = "TEXT_DATA", allowableValues = {"TEXT_DATA"})
        public NoteModuleType getNoteModuleType() {
            return NoteModuleType.TEXT_DATA;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "创建情景对话模块请求参数")
    public static class CreateScenarioRecordModuleRequest implements CreateNoteModulePayload {
        @Schema(description = "内容")
        private String content;

        @Override
        @Schema(description = "模块类型", defaultValue = "SCENARIO_RECORD", allowableValues = {"SCENARIO_RECORD"})
        public NoteModuleType getNoteModuleType() {
            return NoteModuleType.SCENARIO_RECORD;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "创建键值对模块请求参数")
    public static class CreateKeyValueModuleRequest implements CreateNoteModulePayload {
        @Schema(description = "键值对元素列表", nullable = true)
        private List<KeyValueItem> keyValueItems;

        @Override
        @Schema(description = "模块类型", defaultValue = "KEY_VALUE_PAIR", allowableValues = {"KEY_VALUE_PAIR"})
        public NoteModuleType getNoteModuleType() {
            return NoteModuleType.KEY_VALUE_PAIR;
        }
    }


    @Setter
    @Getter
    @NoArgsConstructor
    @Schema(description = "推荐元素")
    public static class RecommendationItemRequest {
        @Schema(description = "推荐描述")
        private String suggestion;
        @Schema(description = "表情")
        private String emoji;
        @Schema(description = "推荐类型")
        private RecommendationItemType recommendType;
        @Schema(description = "商品推荐关键词", nullable = true)
        private String productKeyword;
        @Schema(description = "推荐商品", nullable = true)
        private List<ProductRecommendationItem> productRecommendations;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @Schema(description = "待办元素")
    public static class BacklogItemRequest {
        @Schema(description = "待办内容")
        private String content;
        @Schema(description = "待办描述", nullable = true)
        private String description;
        @Schema(description = "待办时间")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime scheduledAt;
        @Schema(description = "是否需要通知,由用户设置，false表示不需要通知, true表示需要通知")
        private boolean needNotify;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @Schema(description = "推荐元素")
    public static class QAItemRequest {
        @Schema(description = "问题")
        private String question;

        @Schema(description = "答案")
        private String answer;
    }


}
