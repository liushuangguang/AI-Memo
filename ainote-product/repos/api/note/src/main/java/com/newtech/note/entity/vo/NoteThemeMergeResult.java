package com.newtech.note.entity.vo;

import com.newtech.note.entity.dto.Note;
import com.newtech.note.entity.dto.NoteThemeMergeHistory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteThemeMergeResult {
    private String themeId;
    private Note note;
    private NoteThemeMergeHistory history;
}
