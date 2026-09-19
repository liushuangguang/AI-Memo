package com.newtech.note.entity.filter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NoteAnalysisFilter {
    private Integer noteType;

    private String keyword;

    private String deviceId;

    private Integer dimension;
}
