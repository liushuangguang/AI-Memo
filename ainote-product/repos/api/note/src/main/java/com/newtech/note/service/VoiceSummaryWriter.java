package com.newtech.note.service;

import com.newtech.note.entity.dto.Note;
import reactor.core.publisher.Mono;

public interface VoiceSummaryWriter {
    Mono<Note> compareAndSetTextContent(String noteId,
                                        String ownerId,
                                        String moduleId,
                                        int moduleIndex,
                                        String expectedContent,
                                        String newContent,
                                        String saveRequestId,
                                        String summaryDigest);

    Mono<Note> findOwnedActiveNote(String noteId, String ownerId);
}
