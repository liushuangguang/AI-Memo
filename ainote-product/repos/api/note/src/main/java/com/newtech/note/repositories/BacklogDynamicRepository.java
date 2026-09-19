package com.newtech.note.repositories;

import com.newtech.note.entity.dto.Backlog;
import com.newtech.note.entity.filter.BacklogFilter;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BacklogDynamicRepository {
    Flux<Backlog> findByDynamicCriteria(BacklogFilter noteFilter, Pageable pageable);

    Mono<Long> countByDynamicCriteria(BacklogFilter noteFilter);
}
