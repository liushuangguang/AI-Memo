package com.newtech.note.entity.dto.noteModules;

import com.newtech.note.common.enumeration.NoteModuleType;
import com.newtech.note.entity.dto.noteModules.items.KeyValueItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

import java.util.List;

@Setter
@Getter
@BsonDiscriminator(value = "NOTE_MODULE.KEY_VALUE_PAIR")
@Schema(description = "键值对模块")
public class KeyValueModule implements NoteModule {

    @Schema(description = "键值对模块ID，一般为系统维度备忘录的模块ID")
    private String moduleId;

    @Schema(description = "来源自备忘录的id，用于关联")
    private String sourceNoteId;


    @Schema(description = "模块类型")
    private NoteModuleType noteModuleType = NoteModuleType.KEY_VALUE_PAIR;

    @Schema(description = "键值对列表")
    private List<KeyValueItem> items;
}
