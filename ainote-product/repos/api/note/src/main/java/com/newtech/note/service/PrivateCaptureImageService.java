package com.newtech.note.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

public interface PrivateCaptureImageService {
    Mono<StoredCaptureImage> store(String ownerId, FilePart file);

    Mono<ResponseEntity<Resource>> getOwned(String ownerId, String imageId);

    Mono<byte[]> readOwnedBytes(String ownerId, String imageId);

    Mono<Void> deleteOwned(String ownerId, String imageId);

    record StoredCaptureImage(String id, String url, String mimeType, Path localPath) {
    }
}
