package com.newtech.note.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteThemeSourceRef {
    private String noteId;
    private String title;
}
