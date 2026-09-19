package com.newtech.note.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "辅助内容请求")
public class RewriteNoteRequest {
    @Schema(description = "note_assist_record表id")
    private String recordId;
    @Schema(description = "协助方式及类型")
    private List<String> assistDirections;

    @Schema(description = "选中的内容", nullable = true)
    private String selectedContent;
}
