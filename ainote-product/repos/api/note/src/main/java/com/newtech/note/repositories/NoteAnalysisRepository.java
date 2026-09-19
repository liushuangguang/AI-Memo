package com.newtech.note.repositories;

import com.newtech.note.entity.dto.NoteAnalysis;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface NoteAnalysisRepository extends R2dbcRepository<NoteAnalysis, Long>, NoteAnalysisDynamicRepository {
    Flux<NoteAnalysis> findByDeviceId(String deviceId);


    Flux<NoteAnalysis> findByDeviceIdAndNoteType(String deviceId, int noteType, Pageable pageable);

    Mono<NoteAnalysis> insert(NoteAnalysis noteAnalysis);

}
