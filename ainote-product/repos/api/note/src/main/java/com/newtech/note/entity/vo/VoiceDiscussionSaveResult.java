package com.newtech.note.entity.vo;

import com.newtech.note.entity.dto.Note;

public record VoiceDiscussionSaveResult(String noteId,
                                        String saveRequestId,
                                        boolean alreadySaved,
                                        Note note) {
}
