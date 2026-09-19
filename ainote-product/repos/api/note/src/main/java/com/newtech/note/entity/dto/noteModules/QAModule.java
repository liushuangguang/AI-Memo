package com.newtech.note.entity.dto.noteModules;


import com.newtech.note.common.enumeration.NoteModuleType;
import com.newtech.note.entity.dto.noteModules.items.QAItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

import java.util.List;

@Setter
@Getter
@BsonDiscriminator(value = "NOTE_MODULE.QUESTION_ANSWER")
@Schema(description = "AI图片模块")
public class QAModule implements NoteModule {
    @Schema(description = "推荐商品模块ID")
    private String moduleId;

    @Schema(description = "模块类型")
    private NoteModuleType noteModuleType = NoteModuleType.QUESTION_ANSWER;

    @Schema(description = "问答列表，当前只支持单个问答")
    private List<QAItem> items;
}

