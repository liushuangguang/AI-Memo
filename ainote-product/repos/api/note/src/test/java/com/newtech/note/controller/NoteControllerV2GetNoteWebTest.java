package com.newtech.note.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtech.note.config.Filter.MyWebFilter;
import com.newtech.note.entity.dto.Note;
import com.newtech.note.exception.NoteGlobalExceptionHandler;
import com.newtech.note.repositories.NoteRepository;
import com.newtech.note.service.FileUploadService;
import com.newtech.note.service.impl.NoteServiceImplV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NoteControllerV2GetNoteWebTest {
    private NoteRepository repository;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        repository = mock(NoteRepository.class);
        NoteServiceImplV2 service = new NoteServiceImplV2(
                repository, mock(FileUploadService.class), new ObjectMapper());
        client = WebTestClient.bindToController(new NoteControllerV2(service))
                .controllerAdvice(new NoteGlobalExceptionHandler())
                .configureClient()
                .responseTimeout(Duration.ofSeconds(4))
                .build();
    }

    @Test
    void missingNoteReturnsFixedHttp404Body() {
        when(repository.findById("missing-note")).thenReturn(Mono.empty());

        client.get().uri("/v2/note/missing-note")
                .header(MyWebFilter.AUTHENTICATED_OWNER_HEADER, "owner-a")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class).isEqualTo("Note was not found");
    }

    @Test
    void foreignNoteReturnsFixedHttp403BodyWithoutNoteContent() {
        Note foreignNote = note(
                "private-note", "owner-b", "sensitive title", "sensitive content");
        when(repository.findById("private-note")).thenReturn(Mono.just(foreignNote));

        client.get().uri("/v2/note/private-note")
                .header(MyWebFilter.AUTHENTICATED_OWNER_HEADER, "owner-a")
                .exchange()
                .expectStatus().isForbidden()
                .expectBody(String.class).isEqualTo("Note access is forbidden");
    }

    @Test
    void ownedNoteReturnsHttp200WithNote() {
        Note ownedNote = note("owned-note", "owner-a", "visible title", null);
        when(repository.findById("owned-note")).thenReturn(Mono.just(ownedNote));

        client.get().uri("/v2/note/owned-note")
                .header(MyWebFilter.AUTHENTICATED_OWNER_HEADER, "owner-a")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("owned-note")
                .jsonPath("$.deviceId").isEqualTo("owner-a")
                .jsonPath("$.title").isEqualTo("visible title");
    }

    private Note note(String id, String owner, String title, String content) {
        Note note = new Note();
        note.setId(id);
        note.setDeviceId(owner);
        note.setTitle(title);
        note.setContent(content);
        return note;
    }
}
