package com.newtech.note.entity.dto.noteModules;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.newtech.note.common.enumeration.NoteModuleType;
import com.newtech.note.entity.dto.noteModules.items.NoteModuleItem;
import com.newtech.note.util.NoteDeltaJsonParser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.springframework.data.annotation.Transient;

import java.util.List;

@Getter
@Setter
@BsonDiscriminator(value = "NOTE_MODULE.PRODUCT_RECOMMENDATION")
@Schema(description = "文字内容模块")
public class TextDataModule implements NoteModule {
    @Schema(description = "模块ID")
    private String moduleId;

    @Schema(description = "模块类型")
    private NoteModuleType noteModuleType = NoteModuleType.TEXT_DATA;

    @Schema(description = "模块标题")
    @Transient
    private String title;

    /**
     * Notice that never use this getContent() method in backend directly,
     * this field is saved in delta json format,
     * so if we want to use the text content, we should parse the delta json and get the content.
     * use {@link TextDataModule#getCleanedContent}getCleanedContent() method instead.
     */
    @Schema(description = "模块文本内容")
    private String content;

    @Schema(description = "元素列表，目前只有待办元素和推荐元素2种")
    private List<NoteModuleItem> items;

    @Schema(description = "分词结果")
    @JsonIgnore
    private String segmentation;

    public String getCleanedContent() {
        return NoteDeltaJsonParser.parse(content);
    }
}
