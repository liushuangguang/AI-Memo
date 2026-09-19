package com.newtech.note.service.impl;

import com.newtech.note.config.PrivateCaptureStorageProperties;
import com.newtech.note.entity.dto.PrivateCaptureImage;
import com.newtech.note.repositories.PrivateCaptureImageRepository;
import com.newtech.note.service.PrivateCaptureImageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

@Service
public class PrivateCaptureImageServiceImpl implements PrivateCaptureImageService {
    static final long MAX_FILE_BYTES = 5L * 1024L * 1024L;
    static final int MAX_IMAGE_DIMENSION = 20_000;
    static final long MAX_IMAGE_PIXELS = 20_000_000L;

    private final PrivateCaptureImageRepository repository;
    private final Path root;

    public PrivateCaptureImageServiceImpl(PrivateCaptureImageRepository repository,
                                          PrivateCaptureStorageProperties properties) {
        this.repository = repository;
        this.root = properties.directory();
    }

    @Override
    public Mono<StoredCaptureImage> store(String ownerId, FilePart file) {
        if (ownerId == null || ownerId.isBlank()) {
            return Mono.error(new IllegalArgumentException("Capture image owner is required"));
        }
        return Mono.fromCallable(() -> {
                    Files.createDirectories(root);
                    return Files.createTempFile(root, "capture-incoming-", ".tmp");
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(temporary -> file.transferTo(temporary)
                        .then(Mono.fromCallable(() -> validateAndMove(temporary))
                                .subscribeOn(Schedulers.boundedElastic()))
                        .flatMap(stored -> {
                            PrivateCaptureImage metadata = new PrivateCaptureImage(
                                    stored.id(), ownerId, stored.localPath().getFileName().toString(),
                                    stored.mimeType(), LocalDateTime.now());
                            return repository.save(metadata)
                                    .thenReturn(stored)
                                    .onErrorResume(failure -> deleteFile(stored.localPath())
                                            .then(Mono.error(failure)));
                        })
                        .onErrorResume(failure -> deleteFile(temporary).then(Mono.error(failure))));
    }

    @Override
    public Mono<ResponseEntity<Resource>> getOwned(String ownerId, String imageId) {
        return owned(ownerId, imageId).flatMap(metadata -> Mono.fromCallable(() -> {
                    Path path = safePath(metadata.getFileName());
                    if (!Files.isRegularFile(path)) throw notFound();
                    Resource resource = new UrlResource(path.toUri());
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(metadata.getMimeType()))
                            .contentLength(Files.size(path))
                            .cacheControl(CacheControl.noStore().cachePrivate())
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                            .body(resource);
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    public Mono<byte[]> readOwnedBytes(String ownerId, String imageId) {
        return owned(ownerId, imageId).flatMap(metadata -> Mono.fromCallable(() -> {
            Path path = safePath(metadata.getFileName());
            if (!Files.isRegularFile(path) || Files.size(path) > MAX_FILE_BYTES) throw notFound();
            return Files.readAllBytes(path);
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    public Mono<Void> deleteOwned(String ownerId, String imageId) {
        return owned(ownerId, imageId)
                .flatMap(metadata -> deleteFile(safePath(metadata.getFileName()))
                        .then(repository.delete(metadata)))
                .onErrorResume(ResponseStatusException.class,
                        failure -> failure.getStatusCode() == HttpStatus.NOT_FOUND
                                ? Mono.empty()
                                : Mono.error(failure));
    }

    private Mono<PrivateCaptureImage> owned(String ownerId, String imageId) {
        if (imageId == null || !imageId.matches("[A-Za-z0-9-]{1,80}")) {
            return Mono.error(notFound());
        }
        return repository.findById(imageId)
                .filter(metadata -> Objects.equals(ownerId, metadata.getOwnerId()))
                .switchIfEmpty(Mono.error(notFound()));
    }

    private StoredCaptureImage validateAndMove(Path temporary) throws Exception {
        long fileBytes = Files.size(temporary);
        if (fileBytes <= 0 || fileBytes > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("Capture image must be between 1 byte and 5 MB");
        }
        String format;
        int width;
        int height;
        try (ImageInputStream input = ImageIO.createImageInputStream(temporary.toFile())) {
            if (input == null) throw new IllegalArgumentException("Capture image format is invalid");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Capture image format is invalid");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                format = reader.getFormatName().toLowerCase();
                width = reader.getWidth(0);
                height = reader.getHeight(0);
                validateDimensions(width, height);
                if (reader.read(0) == null) {
                    throw new IllegalArgumentException("Capture image could not be decoded");
                }
            } finally {
                reader.dispose();
            }
        }
        String extension;
        String mimeType;
        if ("jpeg".equals(format) || "jpg".equals(format)) {
            extension = "jpg";
            mimeType = MediaType.IMAGE_JPEG_VALUE;
        } else if ("png".equals(format)) {
            extension = "png";
            mimeType = MediaType.IMAGE_PNG_VALUE;
        } else {
            throw new IllegalArgumentException("Only JPEG and PNG capture images are supported");
        }
        String id = UUID.randomUUID().toString();
        Path destination = safePath(id + "." + extension);
        Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
        return new StoredCaptureImage(id, "/v2/capture/images/" + id, mimeType, destination);
    }

    private void validateDimensions(int width, int height) {
        if (width <= 0 || height <= 0 || width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION
                || (long) width * height > MAX_IMAGE_PIXELS) {
            throw new IllegalArgumentException(
                    "Capture image dimensions exceed the 20000-pixel edge or 20-megapixel limit");
        }
    }

    private Path safePath(String fileName) {
        if (fileName == null || !fileName.matches("[A-Za-z0-9.-]{1,120}")) throw notFound();
        Path path = root.resolve(fileName).normalize();
        if (!path.startsWith(root) || path.getParent() == null || !path.getParent().equals(root)) {
            throw notFound();
        }
        return path;
    }

    private Mono<Void> deleteFile(Path path) {
        return Mono.fromRunnable(() -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception failure) {
                        throw new IllegalStateException("Unable to delete private capture image", failure);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Capture image was not found");
    }
}
