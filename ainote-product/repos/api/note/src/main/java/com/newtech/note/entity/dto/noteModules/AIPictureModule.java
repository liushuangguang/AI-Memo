package com.newtech.note.entity.dto.noteModules;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.newtech.note.common.enumeration.NoteModuleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

@Setter
@Getter
@BsonDiscriminator(value = "NOTE_MODULE.AI_PICTURE")
@Schema(description = "AI图片模块")
public class AIPictureModule implements NoteModule {
    @Schema(description = "推荐商品模块ID")
    private String moduleId;

    @Schema(description = "模块类型")
    private NoteModuleType noteModuleType = NoteModuleType.AI_PICTURE;

    @Schema(description = "图片URL")
    private String imageUrl;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "分词结果")
    @JsonIgnore
    private String segmentation;
}
