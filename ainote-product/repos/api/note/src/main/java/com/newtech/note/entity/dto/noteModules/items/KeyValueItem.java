package com.newtech.note.entity.dto.noteModules.items;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.newtech.note.common.enumeration.NoteItemType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "键值对item")
@BsonDiscriminator(value = "NOTE_MODULE_ITEM.KEY_VALUE_PAIR")
public class KeyValueItem implements NoteModuleItem {
    @Schema(description = "item id")
    private String itemId;

    @Schema(description = "item类型")
    private NoteItemType noteItemType = NoteItemType.KEY_VALUE_PAIR;

    @Schema(description = "键, 可能为空, 为空时直接显示value文本", nullable = true)
    private String key;

    @Schema(description = "值")
    private String value;

    @Schema(description = "分词结果", nullable = true)
    @JsonIgnore
    private String segmentation;

    public KeyValueItem(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
