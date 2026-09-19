package com.newtech.note.entity.dto.noteModules.items;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.newtech.note.common.enumeration.NoteItemType;
import com.newtech.note.common.enumeration.RecommendationItemType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@Schema(description = "推荐元素")
@BsonDiscriminator(value = "NOTE_MODULE_ITEM.AI_RECOMMENDATION")
public class RecommendationItem implements NoteModuleItem {
    @Schema(description = "item id")
    private String itemId;

    @Schema(description = "item类型")
    private NoteItemType noteItemType = NoteItemType.AI_RECOMMENDATION;

    @Schema(description = "描述")
    private String suggestion;

    @Schema(description = "emoji")
    private String emoji;

    @Schema(description = "推荐类型")
    private RecommendationItemType recommendType;

    @Schema(description = "商品推荐关键词", nullable = true)
    private String productKeyword;

    @Schema(description = "推荐商品", nullable = true)
    private List<ProductRecommendationItem> productRecommendations;

    @Schema(description = "分词结果", nullable = true)
    @JsonIgnore
    private String segmentation;
}
