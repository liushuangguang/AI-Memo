package com.newtech.note.entity.dto.noteModules.items;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.newtech.note.common.enumeration.NoteItemType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

@Setter
@Getter
@NoArgsConstructor
@Schema(description = "问答元素")
@BsonDiscriminator(value = "NOTE_MODULE_ITEM.QUESTION_ANSWER")
public class QAItem implements NoteModuleItem {
    @Schema(description = "item id")
    private String itemId;

    @Schema(description = "item类型")
    private NoteItemType noteItemType = NoteItemType.QUESTION_ANSWER;

    @Schema(description = "问题")
    private String question;

    @Schema(description = "答案")
    private String answer;

    @Schema(description = "分词结果", nullable = true)
    @JsonIgnore
    private String segmentation;
}
