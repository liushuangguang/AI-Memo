package com.newtech.note.repositories;

import com.newtech.note.entity.dto.NoteAnalysisHistory;
import reactor.core.publisher.Mono;

public interface NoteAnalysisHistoryDynamicRepository {
    Mono<NoteAnalysisHistory> insert(NoteAnalysisHistory entity);
}
