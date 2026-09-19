package com.newtech.note.repositories;

import com.newtech.note.entity.dto.NoteAnalysisHistory;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface NoteAnalysisHistoryRepository extends R2dbcRepository<NoteAnalysisHistory, Long>, NoteAnalysisHistoryDynamicRepository {
    Flux<NoteAnalysisHistory> findByNoteAnalysisId(long noteAnalysisId);

    @Query("select max(version) " +
            "from note_analysis_history h " +
            "where h.note_analysis_id = :noteAnalysisId " +
            "for update")
    Mono<Integer> getLatestVersion(long noteAnalysisId);


    @Query("select * " +
            "from note_analysis_history h " +
            "where h.note_analysis_id = :noteAnalysisId " +
            "and h.analysis_type = :analysisType " +
            "and h.version = :version"/* +
            "for update"*/)
    Mono<NoteAnalysisHistory> findByNoteAnalysisIdAndVersion(long noteAnalysisId, int analysisType, int version);


    @Query(
    "UPDATE note_analysis_history " +
    "SET talk_snapshot = NULL " +
    "WHERE id = :noteAnalysisId ")
    Mono<Integer> deleteNoteDiscussAudio(long noteAnalysisId);
}
