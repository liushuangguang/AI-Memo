package com.newtech.note.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "备忘录校验结果")
public class ValidateNoteResult {
    @Schema(description = "是否有意义")
    private boolean isMeaningful;
    @Schema(description = "校验结果")
    private String result;
    @Schema(description = "备忘录分析/辅助记录ID，如果备忘录有意义，则自动创建一个新的分析/辅助记录。" +
            "后续的AI分析/辅助结果将会关联到这个ID，因此每次进行AI分析整理/辅助时需要将这个ID传入。")
    private String recordId;

}
