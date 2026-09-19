package com.newtech.note.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "note相关链接")
public class NoteRelatedLink {
    /*@Schema(description = "note_analysis表的id")
    private long noteId;*/

    @Schema(description = "文本描述")
    private String text;

    @Schema(description = "链接地址")
    private String link;
}
