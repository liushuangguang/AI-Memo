package com.newtech.note.client.entity.embedding;

import java.util.List;

public record InsertNoteReq(String user_id, List<Note> note) {
    public record Note(String note_id, String note_content, String note_theme) {
    }
}
