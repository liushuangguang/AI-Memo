package com.newtech.note.entity.dto.noteModules;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.newtech.note.common.enumeration.NoteModuleType;
import com.newtech.note.entity.dto.noteModules.items.BacklogItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

import java.util.List;

@Setter
@Getter
@BsonDiscriminator(value = "NOTE_MODULE.BACKLOG")
@Schema(description = "待办模块")
public class BacklogModule implements NoteModule {

    @Schema(description = "推荐商品模块ID")
    private String moduleId;

    @Schema(description = "模块类型")
    private NoteModuleType noteModuleType = NoteModuleType.BACKLOG;

    @Schema(description = "待办模块名称")
    private String name;

    @Schema(description = "待办列表")
    private List<BacklogItem> items;

    @Schema(description = "分词结果")
    @JsonIgnore
    private String segmentation;

}
