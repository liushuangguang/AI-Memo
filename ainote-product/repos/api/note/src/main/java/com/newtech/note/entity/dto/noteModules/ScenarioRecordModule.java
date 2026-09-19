package com.newtech.note.entity.dto.noteModules;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.newtech.note.common.enumeration.NoteModuleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

/**
 * why we need this module:
 * 1. 区分情景对话模块，与文本模块存储功能相同，只是前端展示样式不同
 * 2. 后续可以根据情景对话模块的特点，增加更多的功能，如：情景对话的情感分析、情景对话的情感推荐等
 *
 * @author jiajun
 * @date 2021/05/07
 */
@Getter
@Setter
@BsonDiscriminator(value = "NOTE_MODULE.SCENARIO_RECORD")
@Schema(description = "情景对话模块")
public class ScenarioRecordModule implements NoteModule {
    @Schema(description = "模块ID")
    private String moduleId;

    @Schema(description = "模块类型")
    private NoteModuleType noteModuleType = NoteModuleType.SCENARIO_RECORD;

    @Schema(description = "模块文本内容")
    private String content;

    @Schema(description = "分词结果")
    @JsonIgnore
    private String segmentation;
}
