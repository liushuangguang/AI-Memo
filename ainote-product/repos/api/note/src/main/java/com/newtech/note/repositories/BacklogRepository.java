package com.newtech.note.repositories;

import com.newtech.note.entity.dto.Backlog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface BacklogRepository extends ReactiveMongoRepository<Backlog, String>, BacklogDynamicRepository {
    Flux<Backlog> findByDeviceId(String deviceId);

    Flux<Backlog> findByDeviceId(String deviceId, Pageable pageable);
}
