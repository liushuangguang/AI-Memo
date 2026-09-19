package com.newtech.note.repositories;

import com.newtech.note.entity.dto.NoteAssistRecord;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface NoteAssistRecordRepository extends ReactiveMongoRepository<NoteAssistRecord, String>, NoteAssistRecordDynamicRepository {
    Flux<NoteAssistRecord> findByNoteId(String noteId);

    Mono<NoteAssistRecord> findByNoteIdAndVersion(String noteId, int version);
}
