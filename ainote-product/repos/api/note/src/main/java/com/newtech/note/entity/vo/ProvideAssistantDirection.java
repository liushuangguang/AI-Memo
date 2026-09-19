package com.newtech.note.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@Schema(description = "提供的协助方向")
public class ProvideAssistantDirection {
    @Schema(description = "可提供的协助方向")
    private List<String> availableAssistantDirection;
    @Schema(description = "理由")
    private String reason;
}
