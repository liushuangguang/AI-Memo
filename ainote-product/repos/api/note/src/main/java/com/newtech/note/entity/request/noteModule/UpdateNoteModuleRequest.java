package com.newtech.note.entity.request.noteModule;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.newtech.note.common.enumeration.NoteModuleType;
import com.newtech.note.common.enumeration.RecommendationItemType;
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
@Schema(description = "更新笔记模块请求参数")
public class UpdateNoteModuleRequest {
    @Schema(description = "笔记id")
    private String id;
    @Schema(description = "笔记模块id")
    private String moduleId;
    @Schema(description = "笔记模块")
    private UpdateNoteModulePayload module;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "noteModuleType")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = UpdateAIPictureModuleRequest.class, name = "AI_PICTURE"),
            @JsonSubTypes.Type(value = UpdateAIRecommendationModuleRequest.class, name = "AI_RECOMMENDATION"),
            @JsonSubTypes.Type(value = UpdateBacklogModuleRequest.class, name = "BACKLOG"),
            @JsonSubTypes.Type(value = UpdateQAModuleRequest.class, name = "QUESTION_ANSWER"),
            @JsonSubTypes.Type(value = UpdateRelatedNoteModuleRequest.class, name = "RELATED_NOTE"),
            @JsonSubTypes.Type(value = UpdateScenarioRecordModuleRequest.class, name = "SCENARIO_RECORD"),
            @JsonSubTypes.Type(value = UpdateTextDataModuleRequest.class, name = "TEXT_DATA")
    })
    @Schema(description = "笔记模块接口，用于实现不同的笔记模块类型")
    public interface UpdateNoteModulePayload {
        @Schema(description = "模块透传类型")
        NoteModuleType getNoteModuleType();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "更新AI图片模块请求参数")
    public static class UpdateAIPictureModuleRequest implements UpdateNoteModulePayload {
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
    public static class UpdateAIRecommendationModuleRequest implements UpdateNoteModulePayload {
        @Schema(description = "标题")
        private String title;
        /*@Schema(description = "推荐元素列表")
        private List<RecommendationItemRequest> recommendationItems;*/

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
    public static class UpdateBacklogModuleRequest implements UpdateNoteModulePayload {
        @Schema(description = "名称")
        private String name;
        /*@Schema(description = "代办事项列表")
        private List<BacklogItemRequest> backlogItems;*/

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
    public static class UpdateQAModuleRequest implements UpdateNoteModulePayload {
        /*@Schema(description = "标题")
        private String title;

        @Schema(description = "问答元素列表")
        private List<QAItem> questionAnswerItems;*/

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
    public static class UpdateRelatedNoteModuleRequest implements UpdateNoteModulePayload {
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
    public static class UpdateTextDataModuleRequest implements UpdateNoteModulePayload {
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
    public static class UpdateScenarioRecordModuleRequest implements UpdateNoteModulePayload {
        @Schema(description = "内容")
        private String content;

        @Override
        @Schema(description = "模块类型", defaultValue = "SCENARIO_RECORD", allowableValues = {"SCENARIO_RECORD"})
        public NoteModuleType getNoteModuleType() {
            return NoteModuleType.SCENARIO_RECORD;
        }
    }


    @Setter
    @Getter
    @NoArgsConstructor
    @Schema(description = "推荐元素")
    public static class RecommendationItemRequest {
        @Schema(description = "描述")
        private String description;
        @Schema(description = "推荐类型， 如果设置了商品推荐关键词及推荐商品，则推荐类型必须为PRODUCT")
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
        @Schema(description = "待办时间")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime scheduledAt;
        @Schema(description = "是否需要通知,由用户设置，false表示不需要通知, true表示需要通知")
        private boolean needNotify;
        @Schema(description = "是否删除, false表示未删除, true表示已删除", allowableValues = {"true", "false"}, defaultValue = "false")
        private boolean deleted;
        @Schema(description = "是否添加删除线, false表示未添加, true表示已添加", allowableValues = {"true", "false"}, defaultValue = "false")
        private boolean strikethrough;
        @Schema(description = "是否完成")
        private boolean done;
    }

}
