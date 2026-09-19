package com.newtech.note.repositories;

import com.newtech.note.entity.dto.Note;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface NoteRepository extends ReactiveMongoRepository<Note, String>, NoteDynamicRepository {
    Flux<Note> findByDeviceId(String deviceId);


    Flux<Note> findByDeviceIdAndNoteType(String deviceId, int noteType, Pageable pageable);

    Flux<Note> findByDeviceIdAndDeletedFalseAndDimension(String deviceId, int dimension);

    Mono<Note> findFirstByDeviceIdAndCaptureRequestId(String deviceId, String captureRequestId);

}
