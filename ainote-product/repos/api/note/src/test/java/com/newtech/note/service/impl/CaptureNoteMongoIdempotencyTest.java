package com.newtech.note.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.newtech.note.client.RecognizedTextOrganizer;
import com.newtech.note.entity.dto.Note;
import com.newtech.note.repositories.NoteRepository;
import com.newtech.note.service.FileUploadService;
import com.newtech.note.service.PrivateCaptureImageService;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.repository.support.ReactiveMongoRepositoryFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CaptureNoteMongoIdempotencyTest {
    private MongoServer server;
    private MongoClient client;
    private NoteRepository repository;

    @BeforeEach
    void setUp() {
        server = new MongoServer(new MemoryBackend());
        server.bind("127.0.0.1", 0);
        client = MongoClients.create(server.getConnectionString());
        ReactiveMongoTemplate template = new ReactiveMongoTemplate(client, "capture-note-concurrency");
        // The production repository also has custom search fragments; delegate only
        // the tested persistence operations to a real Mongo template.
        repository = mock(NoteRepository.class);
        when(repository.findFirstByDeviceIdAndCaptureRequestId(anyString(), anyString())).thenAnswer(call ->
                template.findOne(org.springframework.data.mongodb.core.query.Query.query(
                        org.springframework.data.mongodb.core.query.Criteria.where("deviceId").is(call.getArgument(0))
                                .and("captureRequestId").is(call.getArgument(1))), Note.class));
        when(repository.findById(anyString())).thenAnswer(call -> template.findById(call.getArgument(0), Note.class));
        when(repository.insert(org.mockito.ArgumentMatchers.any(Note.class))).thenAnswer(call -> template.insert(call.getArgument(0, Note.class)));
        when(repository.findAll()).thenAnswer(call -> template.findAll(Note.class));
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.shutdown();
    }

    @Test
    void twoServiceInstancesConvergeOnOneMongoNoteForSameOwnerRequest() {
        PrivateCaptureImageService images = mock(PrivateCaptureImageService.class);
        RecognizedTextOrganizer organizer = mock(RecognizedTextOrganizer.class);
        FilePart file = mock(FilePart.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        when(file.headers()).thenReturn(headers);
        when(organizer.organize("设备 OCR 原文")).thenReturn(Mono.just(
                new RecognizedTextOrganizer.OrganizedText("标题", "整理正文")));

        AtomicInteger stores = new AtomicInteger();
        Sinks.One<Void> bothStored = Sinks.one();
        when(images.store(eq("owner"), eq(file))).thenAnswer(ignored -> {
            int number = stores.incrementAndGet();
            if (number == 2) bothStored.tryEmitEmpty();
            return bothStored.asMono().thenReturn(
                    new PrivateCaptureImageService.StoredCaptureImage(
                            "image-" + number,
                            "/v2/capture/images/image-" + number,
                            MediaType.IMAGE_PNG_VALUE,
                            Path.of("image-" + number + ".png")));
        });
        when(images.deleteOwned(anyString(), anyString())).thenReturn(Mono.empty());

        NoteServiceImplV2 first = service(organizer, images);
        NoteServiceImplV2 second = service(organizer, images);
        Mono<Note> firstCreate = first.createImageNote(
                        "owner", file, "设备 OCR 原文", "same-request")
                .map(response -> response.getData())
                .subscribeOn(Schedulers.parallel());
        Mono<Note> secondCreate = second.createImageNote(
                        "owner", file, "设备 OCR 原文", "same-request")
                .map(response -> response.getData())
                .subscribeOn(Schedulers.parallel());

        StepVerifier.create(Mono.zip(firstCreate, secondCreate))
                .assertNext(result -> {
                    assertThat(result.getT1().getId()).isEqualTo(result.getT2().getId());
                    assertThat(result.getT1().getCaptureRequestId()).isEqualTo("same-request");
                })
                .verifyComplete();

        StepVerifier.create(repository.findAll().collectList())
                .assertNext(notes -> {
                    assertThat(notes).hasSize(1);
                    assertThat(notes.getFirst().getDeviceId()).isEqualTo("owner");
                    assertThat(notes.getFirst().getRawOcrText()).isEqualTo("设备 OCR 原文");
                })
                .verifyComplete();
        verify(images, times(2)).store("owner", file);
        verify(images, times(1)).deleteOwned(eq("owner"), anyString());
    }

    private NoteServiceImplV2 service(RecognizedTextOrganizer organizer,
                                      PrivateCaptureImageService images) {
        return new NoteServiceImplV2(
                repository,
                mock(FileUploadService.class),
                new ObjectMapper(),
                null,
                organizer,
                images);
    }
}
