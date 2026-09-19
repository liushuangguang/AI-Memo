package com.newtech.note.repositories;

import com.newtech.note.entity.dto.NoteAnalysisRecord;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface NoteAnalysisRecordRepository extends ReactiveMongoRepository<NoteAnalysisRecord, String>, NoteAnalysisRecordDynamicRepository {
    Flux<NoteAnalysisRecord> findByNoteId(String noteId);

    Mono<NoteAnalysisRecord> findByNoteIdAndVersion(String noteId, int version);

    Mono<NoteAnalysisRecord> findByNoteIdAndSnapshotHash(String noteId, String snapshotHash);
}
