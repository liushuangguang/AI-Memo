package com.newtech.note.repositories;

import com.newtech.note.entity.dto.PrivateCaptureImage;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrivateCaptureImageRepository
        extends ReactiveMongoRepository<PrivateCaptureImage, String> {
}
