package com.newtech.note.repositories;

import com.newtech.note.entity.dto.NoteTheme;
import com.newtech.note.entity.filter.NoteThemeFilter;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NoteThemeDynamicRepository {
    Flux<NoteTheme> findByDynamicCriteria(NoteThemeFilter noteThemeFilter, Pageable pageable);

    Mono<Long> countByDynamicCriteria(NoteThemeFilter noteFilter);
}
