package com.newtech.note.repositories;

import com.newtech.note.entity.dto.Note;
import com.newtech.note.entity.filter.NoteAnalysisFilter;
import com.newtech.note.entity.filter.NoteFilterV2;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NoteDynamicRepository {
    Flux<Note> findByDynamicCriteria(NoteFilterV2 noteFilter, Pageable pageable);

    Mono<Long> countByDynamicCriteria(NoteFilterV2 noteFilter);
}
