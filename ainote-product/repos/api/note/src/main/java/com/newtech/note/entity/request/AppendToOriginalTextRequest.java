package com.newtech.note.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "追加到原始文本请求体")
public class AppendToOriginalTextRequest {

    @Schema(description = "note_analysis表的id, 用于找到对应的note, 并将toAppendText追加到对应的raw_note中")
    private long id;

    @Schema(description = "需要追加的文本")
    private String toAppendText;
}
