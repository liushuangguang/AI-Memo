package com.newtech.note.entity.dto.noteModules;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.newtech.note.common.enumeration.NoteModuleType;
import com.newtech.note.entity.dto.noteModules.items.RecommendationItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

import java.util.List;

@Getter
@Setter
@BsonDiscriminator(value = "NOTE_MODULE.AI_RECOMMENDATION")
@Schema(description = "AI推荐模块")
public class AIRecommendationModule implements NoteModule {
    @Schema(description = "模块ID")
    private String moduleId;

    @Schema(description = "模块类型")
    private NoteModuleType noteModuleType = NoteModuleType.AI_RECOMMENDATION;

    @Schema(description = "模块标题")
    private String title;

    @Schema(description = "推荐列表")
    private List<RecommendationItem> items;

    @Schema(description = "分词结果")
    @JsonIgnore
    private String segmentation;
}
