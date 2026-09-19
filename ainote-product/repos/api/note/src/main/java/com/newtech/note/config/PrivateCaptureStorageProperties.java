package com.newtech.note.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/** Filesystem location for owner-scoped capture images. It is never registered as a static resource. */
@Component
public final class PrivateCaptureStorageProperties {
    private final Path directory;

    public PrivateCaptureStorageProperties(
            @Value("${capture.images.private-dir:private-capture-images}") String configuredDirectory) {
        if (configuredDirectory == null || configuredDirectory.isBlank()) {
            throw new IllegalArgumentException("capture.images.private-dir must not be blank");
        }
        try {
            this.directory = Path.of(configuredDirectory.trim()).toAbsolutePath().normalize();
        } catch (InvalidPathException failure) {
            throw new IllegalArgumentException("capture.images.private-dir is invalid", failure);
        }
    }

    public Path directory() {
        return directory;
    }
}
