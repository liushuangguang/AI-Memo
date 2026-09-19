package com.newtech.note.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtech.note.client.ImageRecognitionClient;
import com.newtech.note.client.RecognizedTextOrganizer;
import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.entity.dto.Note;
import com.newtech.note.repositories.NoteRepository;
import com.newtech.note.service.FileUploadService;
import com.newtech.note.service.PrivateCaptureImageService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NoteServiceImplV2ImageRecognitionTest {
    @Test
    void createsPersistedNoteFromValidatedInlineRecognition() {
        NoteRepository repository = mock(NoteRepository.class);
        FileUploadService upload = mock(FileUploadService.class);
        PrivateCaptureImageService privateImages = mock(PrivateCaptureImageService.class);
        ImageRecognitionClient recognition = mock(ImageRecognitionClient.class);
        FilePart file = mock(FilePart.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        when(file.headers()).thenReturn(headers);
        when(repository.findFirstByDeviceIdAndCaptureRequestId("owner", "request-1"))
                .thenReturn(Mono.empty());
        when(privateImages.store("owner", file)).thenReturn(Mono.just(
                new PrivateCaptureImageService.StoredCaptureImage(
                        "image-1", "/v2/capture/images/image-1", "image/png", Path.of("image.png"))));
        when(privateImages.readOwnedBytes("owner", "image-1")).thenReturn(Mono.just(new byte[]{1, 2, 3}));
        when(recognition.recognizeImageBytes(any(), eq("image/png"))).thenReturn(Mono.just(
                new ImageRecognitionClient.ImageRecognitionResult(
                        "原始识别文字", "会议安排", "明天十点参加产品会议")));
        when(repository.insert(any(Note.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        NoteServiceImplV2 service = new NoteServiceImplV2(
                repository, upload, new ObjectMapper(), recognition, null, privateImages);

        StepVerifier.create(service.createImageNote("owner", file, null, "request-1"))
                .assertNext(response -> {
                    assertEquals(200, response.getCode());
                    Note note = response.getData();
                    assertNotNull(note.getId());
                    assertEquals("owner", note.getDeviceId());
                    assertEquals("会议安排", note.getTitle());
                    assertEquals("明天十点参加产品会议", note.getContent());
                    assertEquals("/v2/capture/images/image-1", note.getImageUrl());
                    assertEquals("原始识别文字", note.getRawOcrText());
                    assertEquals("request-1", note.getCaptureRequestId());
                })
                .verifyComplete();
        verify(recognition).recognizeImageBytes(any(), eq("image/png"));
        verify(upload, never()).compressImage(any());
        verify(repository).insert(any(Note.class));
    }

    @Test
    void usesDeviceOcrTextWithoutRequiringBailianImageAccess() {
        NoteRepository repository = mock(NoteRepository.class);
        FileUploadService upload = mock(FileUploadService.class);
        PrivateCaptureImageService privateImages = mock(PrivateCaptureImageService.class);
        RecognizedTextOrganizer organizer = mock(RecognizedTextOrganizer.class);
        FilePart file = mock(FilePart.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        when(file.headers()).thenReturn(headers);
        when(repository.findFirstByDeviceIdAndCaptureRequestId("owner", "request-2"))
                .thenReturn(Mono.empty());
        when(privateImages.store("owner", file)).thenReturn(Mono.just(
                new PrivateCaptureImageService.StoredCaptureImage(
                        "image-2", "/v2/capture/images/image-2", "image/jpeg", Path.of("image.jpg"))));
        when(organizer.organize("设备真实 OCR 文本")).thenReturn(Mono.just(
                new RecognizedTextOrganizer.OrganizedText("OCR 笔记", "整理后的 OCR 正文")));
        when(repository.insert(any(Note.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        NoteServiceImplV2 service = new NoteServiceImplV2(
                repository, upload, new ObjectMapper(), null, organizer, privateImages);

        StepVerifier.create(service.createImageNote(
                        "owner", file, "设备真实 OCR 文本", "request-2"))
                .assertNext(response -> {
                    assertEquals(200, response.getCode());
                    assertEquals("OCR 笔记", response.getData().getTitle());
                    assertEquals("整理后的 OCR 正文", response.getData().getContent());
                    assertEquals("设备真实 OCR 文本", response.getData().getRawOcrText());
                })
                .verifyComplete();
        verify(organizer).organize("设备真实 OCR 文本");
        verify(privateImages, never()).readOwnedBytes(any(), any());
    }

    @Test
    void retriesSameRequestWithoutCreatingDuplicateNoteOrImage() {
        NoteRepository repository = mock(NoteRepository.class);
        FileUploadService upload = mock(FileUploadService.class);
        PrivateCaptureImageService privateImages = mock(PrivateCaptureImageService.class);
        RecognizedTextOrganizer organizer = mock(RecognizedTextOrganizer.class);
        FilePart file = mock(FilePart.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        when(file.headers()).thenReturn(headers);
        AtomicReference<Note> saved = new AtomicReference<>();
        when(repository.findFirstByDeviceIdAndCaptureRequestId("owner", "stable-request"))
                .thenAnswer(ignored -> Mono.justOrEmpty(saved.get()));
        when(privateImages.store("owner", file)).thenReturn(Mono.just(
                new PrivateCaptureImageService.StoredCaptureImage(
                        "image-3", "/v2/capture/images/image-3", "image/jpeg", Path.of("image.jpg"))));
        when(organizer.organize("raw text")).thenReturn(Mono.just(
                new RecognizedTextOrganizer.OrganizedText("title", "content")));
        when(repository.insert(any(Note.class))).thenAnswer(invocation -> {
            Note note = invocation.getArgument(0);
            saved.set(note);
            return Mono.just(note);
        });
        NoteServiceImplV2 service = new NoteServiceImplV2(
                repository, upload, new ObjectMapper(), null, organizer, privateImages);

        StepVerifier.create(service.createImageNote(
                        "owner", file, "raw text", "stable-request"))
                .expectNextMatches(NoteBaseResponse::isSuccess)
                .verifyComplete();
        StepVerifier.create(service.createImageNote(
                        "owner", file, "raw text", "stable-request"))
                .expectNextMatches(response -> response.isSuccess()
                        && response.getData().getId().equals(saved.get().getId()))
                .verifyComplete();

        verify(privateImages, times(1)).store("owner", file);
        verify(repository, times(1)).insert(any(Note.class));
    }

    @Test
    void deletesPrivateImageWhenOrganizerFails() {
        NoteRepository repository = mock(NoteRepository.class);
        FileUploadService upload = mock(FileUploadService.class);
        PrivateCaptureImageService privateImages = mock(PrivateCaptureImageService.class);
        RecognizedTextOrganizer organizer = mock(RecognizedTextOrganizer.class);
        FilePart file = mock(FilePart.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        when(file.headers()).thenReturn(headers);
        when(repository.findFirstByDeviceIdAndCaptureRequestId(any(), any())).thenReturn(Mono.empty());
        when(privateImages.store("owner", file)).thenReturn(Mono.just(
                new PrivateCaptureImageService.StoredCaptureImage(
                        "image-fail", "/v2/capture/images/image-fail", "image/png", Path.of("image.png"))));
        when(organizer.organize("raw text")).thenReturn(Mono.error(new IllegalStateException("boom")));
        when(privateImages.deleteOwned("owner", "image-fail")).thenReturn(Mono.empty());
        NoteServiceImplV2 service = new NoteServiceImplV2(
                repository, upload, new ObjectMapper(), null, organizer, privateImages);

        StepVerifier.create(service.createImageNote("owner", file, "raw text", "request-fail"))
                .expectNextMatches(response -> !response.isSuccess())
                .verifyComplete();

        verify(privateImages).deleteOwned("owner", "image-fail");
        verify(repository, never()).insert(any(Note.class));
    }
}
