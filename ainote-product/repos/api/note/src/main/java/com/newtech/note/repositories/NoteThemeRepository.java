package com.newtech.note.repositories;

import com.newtech.note.entity.dto.NoteTheme;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface NoteThemeRepository extends ReactiveMongoRepository<NoteTheme, String>, NoteThemeDynamicRepository {
    Flux<NoteTheme> findByDeviceId(String deviceId);
}
