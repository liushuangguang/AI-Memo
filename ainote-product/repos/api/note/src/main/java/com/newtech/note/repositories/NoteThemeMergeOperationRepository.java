package com.newtech.note.repositories;

import com.newtech.note.entity.dto.NoteThemeMergeOperation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NoteThemeMergeOperationRepository
        extends ReactiveMongoRepository<NoteThemeMergeOperation, String> {
}
