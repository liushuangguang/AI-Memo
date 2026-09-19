package com.newtech.note.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtech.note.client.CozeClient;
import com.newtech.note.common.BusinessException;
import com.newtech.note.config.ImageFallbackStorageProperties;
import com.newtech.note.service.ImageGenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ImageGenServiceImpl implements ImageGenService {

    private static final Pattern HTTP_URL_IN_TEXT = Pattern.compile(
            "https?://[^\\s\\\"'<>\\\\]+", Pattern.CASE_INSENSITIVE);

    private static final int FALLBACK_WIDTH = 640;
    private static final int FALLBACK_HEIGHT = 360;
    private static final Object FALLBACK_WRITE_LOCK = new Object();
    private static final String DEFAULT_TRUSTED_HOST_SUFFIXES =
            "s.coze.cn,coze.cn,byteimg.com,volces.com,ibytedtos.com";

    @Value("${workflow_id.image.gen:7379868069084069924}")
    private String workflowId;
    @Value("${note.hostName:http://localhost:8080/}")
    private String hostName = "http://localhost:8080/";
    @Value("${image.gen.trusted-host-suffixes:" + DEFAULT_TRUSTED_HOST_SUFFIXES + "}")
    private String trustedHostSuffixes = DEFAULT_TRUSTED_HOST_SUFFIXES;
    private final ObjectMapper objectMapper;
    private final CozeClient cozeClient;
    private final ImageFallbackStorageProperties fallbackStorage;

    @Autowired
    public ImageGenServiceImpl(ObjectMapper objectMapper, CozeClient cozeClient,
                               ImageFallbackStorageProperties fallbackStorage) {
        this.cozeClient = cozeClient;
        this.objectMapper = objectMapper;
        this.fallbackStorage = fallbackStorage;
    }

    ImageGenServiceImpl(ObjectMapper objectMapper, CozeClient cozeClient) {
        this(objectMapper, cozeClient, new ImageFallbackStorageProperties(
                "upload-files", "upload-files"));
    }

    ImageGenServiceImpl(ObjectMapper objectMapper, CozeClient cozeClient,
                        Path fallbackDirectory, String fallbackHostName, String fallbackPath) {
        this(objectMapper, cozeClient, fallbackDirectory, fallbackHostName,
                fallbackPath, DEFAULT_TRUSTED_HOST_SUFFIXES);
    }

    ImageGenServiceImpl(ObjectMapper objectMapper, CozeClient cozeClient,
                        Path fallbackDirectory, String fallbackHostName,
                        String fallbackPath, String trustedHostSuffixes) {
        this(objectMapper, cozeClient, new ImageFallbackStorageProperties(
                fallbackDirectory.toString(), fallbackPath), fallbackHostName, trustedHostSuffixes);
    }

    ImageGenServiceImpl(ObjectMapper objectMapper, CozeClient cozeClient,
                        ImageFallbackStorageProperties fallbackStorage,
                        String fallbackHostName, String trustedHostSuffixes) {
        this(objectMapper, cozeClient, fallbackStorage);
        this.hostName = fallbackHostName;
        this.trustedHostSuffixes = trustedHostSuffixes;
    }

    @Override
    public Mono<String> gen(String input, ServerHttpRequest req) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("original_note", input);
        return cozeClient.callCozeWorkflowApiFiltered(workflowId, "", parameters, null, req).flatMap(response -> {
            try {
                JsonNode jsonNode = objectMapper.readTree(response);
                Optional<String> imageUrl = jsonNode == null
                        ? Optional.empty()
                        : firstHttpUrl(jsonNode.get("images1"));
                if (imageUrl.isEmpty() && jsonNode != null) {
                    imageUrl = firstHttpUrl(jsonNode.get("output"));
                }
                if (imageUrl.isEmpty() && jsonNode != null) {
                    imageUrl = firstHttpUrlFromKnownTextPayload(jsonNode.get("data"));
                }
                if (imageUrl.isEmpty()) {
                    return Mono.error(new BusinessException("COZE_IMAGE_RESPONSE_INVALID",
                            "Coze image response contains no valid HTTP image URL"));
                }
                return Mono.just(imageUrl.get());
            } catch (JsonProcessingException e) {
                BusinessException failure = new BusinessException("COZE_IMAGE_RESPONSE_INVALID",
                        "Coze image response is not valid JSON");
                failure.initCause(e);
                return Mono.error(failure);
            }
        }).onErrorResume(failure -> fallbackForCozeFailure(input, failure));
    }

    private Mono<String> fallbackForCozeFailure(String input, Throwable failure) {
        if (!isGenuineCozeFailure(failure)) {
            return Mono.error(failure);
        }
        return Mono.fromCallable(() -> writeFallbackImage(input))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private boolean isGenuineCozeFailure(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof BusinessException businessException
                    && businessException.getCode() != null
                    && businessException.getCode().startsWith("COZE_")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * Degraded local fallback only: the pixels may be content-derived, but the
     * capability URL is random per call and contains no memo digest or text.
     */
    private String writeFallbackImage(String input) throws IOException {
        synchronized (FALLBACK_WRITE_LOCK) {
            Path directory = fallbackStorage.uploadDirectory();
            Files.createDirectories(directory);
            String filename = "ai-illustration-fallback-" + UUID.randomUUID() + ".png";
            Path target = directory.resolve(filename);
            Path temporary = Files.createTempFile(directory, filename + ".", ".tmp");
            try {
                writePng(temporary, input);
                try {
                    Files.move(temporary, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temporary, target);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
            return joinUrl(hostName, fallbackStorage.urlPath(), filename);
        }
    }

    private void writePng(Path path, String input) throws IOException {
        BufferedImage image = new BufferedImage(FALLBACK_WIDTH, FALLBACK_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(245, 241, 232));
            graphics.fillRect(0, 0, FALLBACK_WIDTH, FALLBACK_HEIGHT);
            String variation = sha256("ai-illustration-fallback|" + String.valueOf(input));
            graphics.setColor(new Color(35 + Integer.parseInt(variation.substring(0, 2), 16) % 32,
                    55 + Integer.parseInt(variation.substring(2, 4), 16) % 32,
                    72 + Integer.parseInt(variation.substring(4, 6), 16) % 32));
            graphics.fillRoundRect(92, 58, 456, 244, 24, 24);
            graphics.setColor(new Color(255, 251, 241));
            graphics.fillRoundRect(124, 90, 392, 180, 14, 14);
            graphics.setColor(new Color(87, 166, 142));
            graphics.fillRoundRect(158, 125, 32, 32, 8, 8);
            graphics.fillRoundRect(158, 178, 32, 32, 8, 8);
            graphics.fillRoundRect(158, 231, 32, 32, 8, 8);
            graphics.setColor(new Color(207, 216, 213));
            graphics.fillRoundRect(214, 130, 240, 12, 6, 6);
            graphics.fillRoundRect(214, 183, 190, 12, 6, 6);
            graphics.fillRoundRect(214, 236, 220, 12, 6, 6);
        } finally {
            graphics.dispose();
        }
        if (!ImageIO.write(image, "png", path.toFile())) {
            throw new IOException("PNG writer unavailable");
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private String joinUrl(String host, String path, String filename) {
        String normalizedHost = host == null ? "" : host.trim().replaceAll("/+\\z", "");
        String normalizedPath = path == null ? "upload-files" : path.trim().replaceAll("^/+|/+$", "");
        return normalizedHost + "/" + normalizedPath + "/" + filename;
    }

    private Optional<String> firstHttpUrl(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        if (node.isTextual()) {
            String value = node.asText().trim();
            if (isSafePublicHttpsUrl(value)) {
                return Optional.of(value);
            }
            if ((value.startsWith("{") && value.endsWith("}"))
                    || (value.startsWith("[") && value.endsWith("]"))) {
                try {
                    return firstHttpUrl(objectMapper.readTree(value));
                } catch (JsonProcessingException ignored) {
                    return Optional.empty();
                }
            }
            return Optional.empty();
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                Optional<String> result = firstHttpUrl(item);
                if (result.isPresent()) {
                    return result;
                }
            }
            return Optional.empty();
        }
        if (node.isObject()) {
            for (String field : new String[]{"url", "image_url", "imageUrl", "image", "images"}) {
                Optional<String> result = firstHttpUrl(node.get(field));
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        return Optional.empty();
    }

    /**
     * The published AI_picture workflow currently returns its generated URL at
     * the end of the known top-level {@code data} field, after human-readable
     * note text. Keep this extraction scoped to that field so unrelated
     * metadata URLs can never become the illustration result.
     */
    private Optional<String> firstHttpUrlFromKnownTextPayload(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        if (!node.isTextual()) {
            return firstHttpUrl(node);
        }

        String value = node.asText().trim();
        Optional<String> directOrJson = firstHttpUrl(node);
        if (directOrJson.isPresent()) {
            return directOrJson;
        }

        Matcher matcher = HTTP_URL_IN_TEXT.matcher(value);
        Optional<String> lastValidUrl = Optional.empty();
        while (matcher.find()) {
            String candidate = stripTrailingPunctuation(matcher.group());
            if (isSafePublicHttpsUrl(candidate)) {
                lastValidUrl = Optional.of(candidate);
            }
        }
        return lastValidUrl;
    }

    private String stripTrailingPunctuation(String value) {
        int end = value.length();
        while (end > 0 && "),.;!?，。；！？）】》」』".indexOf(value.charAt(end - 1)) >= 0) {
            end--;
        }
        return value.substring(0, end);
    }

    private boolean isSafePublicHttpsUrl(String value) {
        try {
            URI uri = URI.create(value);
            if (!uri.isAbsolute() || !"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getRawUserInfo() != null || uri.getRawFragment() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)) {
                return false;
            }

            String rawHost = uri.getHost();
            if (rawHost == null || rawHost.isBlank()) {
                return false;
            }
            String host = stripIpv6Brackets(rawHost).toLowerCase(Locale.ROOT);
            while (host.endsWith(".")) {
                host = host.substring(0, host.length() - 1);
            }
            if (host.isBlank() || "localhost".equals(host)
                    || host.endsWith(".localhost") || "local".equals(host)
                    || host.endsWith(".local")) {
                return false;
            }

            if (host.indexOf(':') >= 0 || host.matches("[0-9.]+")
                    || host.matches("(?i)(?:0x[0-9a-f]+|[0-9]+)(?:\\.(?:0x[0-9a-f]+|[0-9]+))*")) {
                return false;
            }
            boolean validHostname = host.length() <= 253
                    && host.contains(".")
                    && host.matches("(?i)[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?")
                    && java.util.Arrays.stream(host.split("\\.", -1))
                    .allMatch(label -> !label.isEmpty() && label.length() <= 63
                            && !label.startsWith("-") && !label.endsWith("-"));
            return validHostname && isTrustedHost(host);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private String stripIpv6Brackets(String host) {
        return host.startsWith("[") && host.endsWith("]")
                ? host.substring(1, host.length() - 1)
                : host;
    }

    private boolean isTrustedHost(String host) {
        if (trustedHostSuffixes == null) {
            return false;
        }
        return java.util.Arrays.stream(trustedHostSuffixes.split(","))
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .map(value -> value.startsWith(".") ? value.substring(1) : value)
                .filter(value -> !value.isBlank())
                .anyMatch(suffix -> host.equals(suffix) || host.endsWith("." + suffix));
    }
}
