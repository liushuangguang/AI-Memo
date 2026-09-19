package com.newtech.note.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Canonical filesystem and public-path configuration for generated fallback images.
 * Both the writer and WebFlux resource handler must use this instance so a URL can
 * never point at a different directory from the one that received the image.
 */
@Component
public final class ImageFallbackStorageProperties {
    static final String DEFAULT_UPLOAD_DIRECTORY = "upload-files";
    static final String DEFAULT_URL_PATH = "upload-files";

    private final Path uploadDirectory;
    private final String urlPath;

    public ImageFallbackStorageProperties(
            @Value("${image.gen.fallback.upload-dir:" + DEFAULT_UPLOAD_DIRECTORY + "}") String uploadDirectory,
            @Value("${image.gen.fallback.url-path:" + DEFAULT_URL_PATH + "}") String urlPath) {
        this.uploadDirectory = normalizeUploadDirectory(uploadDirectory);
        this.urlPath = normalizeUrlPath(urlPath);
    }

    public Path uploadDirectory() {
        return uploadDirectory;
    }

    public String urlPath() {
        return urlPath;
    }

    public String resourceHandlerPattern() {
        return "/" + urlPath + "/**";
    }

    public String resourceLocation() {
        String location = uploadDirectory.toUri().toASCIIString();
        return location.endsWith("/") ? location : location + "/";
    }

    private static Path normalizeUploadDirectory(String configured) {
        if (configured == null || configured.isBlank()) {
            throw new IllegalArgumentException("image.gen.fallback.upload-dir must not be blank");
        }
        try {
            return Path.of(configured.trim()).toAbsolutePath().normalize();
        } catch (InvalidPathException failure) {
            throw new IllegalArgumentException("image.gen.fallback.upload-dir is invalid", failure);
        }
    }

    private static String normalizeUrlPath(String configured) {
        if (configured == null || configured.isBlank()) {
            throw new IllegalArgumentException("image.gen.fallback.url-path must not be blank");
        }
        String value = configured.trim();
        if (value.contains("//") || value.indexOf('\\') >= 0
                || value.chars().anyMatch(character -> "*?{}[]#%".indexOf(character) >= 0)) {
            throw new IllegalArgumentException("image.gen.fallback.url-path contains unsafe syntax");
        }
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("image.gen.fallback.url-path must name a path");
        }
        String[] segments = value.split("/", -1);
        boolean invalid = Arrays.stream(segments).anyMatch(segment -> segment.isBlank()
                || ".".equals(segment) || "..".equals(segment)
                || !segment.matches("[A-Za-z0-9._~-]+"));
        if (invalid) {
            throw new IllegalArgumentException("image.gen.fallback.url-path contains an unsafe segment");
        }
        return String.join("/", segments);
    }
}
