package com.newtech.note.entity.filter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CategorizedNoteFilter {
    private Integer noteType;

    private Long noteAnalysisId;

    private String deviceId;

}
