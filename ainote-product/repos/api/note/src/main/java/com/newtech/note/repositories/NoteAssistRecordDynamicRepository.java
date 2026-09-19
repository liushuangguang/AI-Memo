package com.newtech.note.repositories;

import reactor.core.publisher.Mono;

public interface NoteAssistRecordDynamicRepository {

    Mono<Integer> getLatestVersion(String noteId);
}
