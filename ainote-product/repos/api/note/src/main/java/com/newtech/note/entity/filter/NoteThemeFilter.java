package com.newtech.note.entity.filter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NoteThemeFilter {
    private String keyword;

    private String deviceId;
}
