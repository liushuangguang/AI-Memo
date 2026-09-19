package com.newtech.note.service.impl;

import com.newtech.note.config.PrivateCaptureStorageProperties;
import com.newtech.note.entity.dto.PrivateCaptureImage;
import com.newtech.note.repositories.PrivateCaptureImageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PrivateCaptureImageServiceImplTest {
    @TempDir
    Path root;

    @Test
    void storesWithDetectedMimeAndServesOnlyOwner() throws Exception {
        PrivateCaptureImageRepository repository = mock(PrivateCaptureImageRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        FilePart file = filePart(pngBytes(2, 3));
        PrivateCaptureImageServiceImpl service = service(repository);

        var stored = service.store("owner-a", file).block();

        assertNotNull(stored);
        assertEquals(MediaType.IMAGE_PNG_VALUE, stored.mimeType());
        assertTrue(stored.url().matches("/v2/capture/images/[A-Za-z0-9-]+"));
        assertTrue(stored.localPath().getFileName().toString().endsWith(".png"));
        assertTrue(Files.isRegularFile(stored.localPath()));

        PrivateCaptureImage metadata = new PrivateCaptureImage(
                stored.id(), "owner-a", stored.localPath().getFileName().toString(),
                stored.mimeType(), LocalDateTime.now());
        when(repository.findById(stored.id())).thenReturn(Mono.just(metadata));
        StepVerifier.create(service.getOwned("owner-b", stored.id()))
                .expectError(ResponseStatusException.class)
                .verify();
        StepVerifier.create(service.getOwned("owner-a", stored.id()))
                .assertNext(response -> {
                    assertEquals(MediaType.IMAGE_PNG, response.getHeaders().getContentType());
                    assertTrue(response.getHeaders().getCacheControl().contains("no-store"));
                })
                .verifyComplete();
    }

    @Test
    void rejectsPathologicalDimensionsBeforeFullDecodeAndLeavesNoFile() {
        PrivateCaptureImageRepository repository = mock(PrivateCaptureImageRepository.class);
        FilePart file = filePart(pathologicalPngHeader(5_000, 5_000));
        PrivateCaptureImageServiceImpl service = service(repository);

        StepVerifier.create(service.store("owner", file))
                .expectErrorMatches(error -> error instanceof IllegalArgumentException
                        && error.getMessage().contains("20-megapixel"))
                .verify();

        assertDoesNotThrow(() -> assertEquals(0L, Files.list(root).count()));
        verify(repository, never()).save(any());
    }

    @Test
    void removesFileWhenMetadataPersistenceFails() throws Exception {
        PrivateCaptureImageRepository repository = mock(PrivateCaptureImageRepository.class);
        when(repository.save(any())).thenReturn(Mono.error(new IllegalStateException("mongo down")));
        PrivateCaptureImageServiceImpl service = service(repository);

        StepVerifier.create(service.store("owner", filePart(pngBytes(1, 1))))
                .expectErrorMessage("mongo down")
                .verify();

        assertDoesNotThrow(() -> assertEquals(0L, Files.list(root).count()));
    }

    private PrivateCaptureImageServiceImpl service(PrivateCaptureImageRepository repository) {
        return new PrivateCaptureImageServiceImpl(
                repository, new PrivateCaptureStorageProperties(root.toString()));
    }

    private FilePart filePart(byte[] bytes) {
        FilePart file = mock(FilePart.class);
        when(file.transferTo(any(Path.class))).thenAnswer(invocation -> {
            Files.write(invocation.getArgument(0), bytes);
            return Mono.empty();
        });
        return file;
    }

    private byte[] pngBytes(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", output));
        return output.toByteArray();
    }

    private byte[] pathologicalPngHeader(int width, int height) {
        try {
            byte[] bytes = pngBytes(1, 1);
            ByteBuffer.wrap(bytes).putInt(16, width).putInt(20, height);
            CRC32 crc = new CRC32();
            crc.update(bytes, 12, 17);
            ByteBuffer.wrap(bytes).putInt(29, (int) crc.getValue());
            return bytes;
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }
}
