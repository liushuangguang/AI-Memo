package com.newtech.note.repositories.impl;

import com.newtech.note.entity.dto.NoteAnalysisHistory;
import com.newtech.note.repositories.NoteAnalysisHistoryDynamicRepository;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Mono;

public class NoteAnalysisHistoryDynamicRepositoryImpl implements NoteAnalysisHistoryDynamicRepository {
    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    public NoteAnalysisHistoryDynamicRepositoryImpl(R2dbcEntityTemplate r2dbcEntityTemplate) {
        this.r2dbcEntityTemplate = r2dbcEntityTemplate;
    }

    @Override
    public Mono<NoteAnalysisHistory> insert(NoteAnalysisHistory entity) {
        return r2dbcEntityTemplate.insert(entity);
    }
}
