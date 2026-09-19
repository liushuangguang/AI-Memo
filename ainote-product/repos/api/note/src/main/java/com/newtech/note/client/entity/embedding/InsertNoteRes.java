package com.newtech.note.client.entity.embedding;

import java.util.List;

public record InsertNoteRes(String userId, List<NoteResult> result) {
    public record NoteResult(String noteId, String relatedThemeId, List<String> relatedNotesIds) {
    }
}