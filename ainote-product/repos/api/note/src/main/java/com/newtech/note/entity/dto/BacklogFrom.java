package com.newtech.note.entity.dto;

import com.newtech.note.common.enumeration.BacklogSource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "待办来源")
public class BacklogFrom {
    @Schema(description = "待办来源")
    private BacklogSource source;
    @Schema(description = "待办来源ID")
    private String sourceId;
}
