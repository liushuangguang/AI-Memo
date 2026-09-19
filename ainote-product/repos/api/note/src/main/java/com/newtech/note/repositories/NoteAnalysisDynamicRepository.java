package com.newtech.note.repositories;

import com.newtech.note.entity.dto.NoteAnalysis;
import com.newtech.note.entity.filter.NoteAnalysisFilter;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NoteAnalysisDynamicRepository {
    Flux<NoteAnalysis> findByDynamicCriteria(NoteAnalysisFilter filter, Pageable pageable);

    Mono<Long> countByDynamicCriteria(NoteAnalysisFilter noteAnalysisFilter);

    Mono<NoteAnalysis> insert(NoteAnalysis entity);
}
