package com.newtech.note.service.impl;

import com.newtech.note.common.BusinessException;
import com.newtech.note.entity.dto.Note;
import com.newtech.note.entity.dto.NoteAssistRecord;
import com.newtech.note.entity.request.ProvideAssistantDirectionRequest;
import com.newtech.note.entity.request.RewriteNoteRequest;
import com.newtech.note.entity.request.ValidateNoteRequest;
import com.newtech.note.security.NoteOwnershipService;
import com.newtech.note.service.DifyNoteService;
import com.newtech.note.service.NoteAssistRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NoteAssistServiceImplTest {
    private NoteAssistRecordService recordService;
    private DifyNoteService difyService;
    private NoteOwnershipService ownershipService;
    private NoteAssistServiceImpl service;

    @BeforeEach
    void setUp() {
        recordService = mock(NoteAssistRecordService.class);
        difyService = mock(DifyNoteService.class);
        ownershipService = mock(NoteOwnershipService.class);
        service = new NoteAssistServiceImpl(recordService, difyService, ownershipService);
    }

    @Test
    void currentAssistValidationStillWorksForOwnedNote() {
        Note note = mock(Note.class);
        when(note.getContent()).thenReturn("meaningful content");
        NoteAssistRecord record = new NoteAssistRecord();
        record.setId("assist-record");
        when(ownershipService.ownedNote(eq("note"), any())).thenReturn(Mono.just(note));
        when(difyService.isNoteMakeSense(eq("meaningful content"), any())).thenReturn(Mono.just("1"));
        when(recordService.create(any())).thenReturn(Mono.just(record));

        StepVerifier.create(service.validateNote(new ValidateNoteRequest("note"), request()))
                .assertNext(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getData().getRecordId()).isEqualTo("assist-record");
                })
                .verifyComplete();
    }

    @Test
    void deniedAssistRecordOperationsNeverReachProviderOrRecordService() {
        when(ownershipService.ownedAssistRecord(anyString(), any())).thenReturn(Mono.error(
                new BusinessException("NOTE_FORBIDDEN", "forbidden")));

        RewriteNoteRequest rewrite = new RewriteNoteRequest("record", List.of("shorter"), null);
        ProvideAssistantDirectionRequest direction = new ProvideAssistantDirectionRequest("record");
        assertForbidden(service.rewriteContent(rewrite, request()));
        assertForbidden(service.provideAssistantDirection(direction, request()));

        verifyNoInteractions(recordService, difyService);
    }

    @Test
    void invalidRewriteProviderOutputIsNeverCached() {
        NoteAssistRecord record = new NoteAssistRecord();
        record.setId("record");
        record.setRawNote("原始笔记");
        when(ownershipService.ownedAssistRecord(eq("record"), any())).thenReturn(Mono.just(record));
        when(difyService.rewriteContent(anyString(), anyString(), anyString(), any()))
                .thenReturn(Mono.error(new IllegalStateException(
                        "AI provider returned invalid output for content rewrite")));

        StepVerifier.create(service.rewriteContent(
                        new RewriteNoteRequest("record", List.of("简洁"), null), request()))
                .expectErrorMessage("AI provider returned invalid output for content rewrite")
                .verify();

        verify(recordService, never()).update(any());
    }

    private MockServerHttpRequest request() {
        return MockServerHttpRequest.post("/note/assist/test").build();
    }

    private void assertForbidden(Mono<?> result) {
        StepVerifier.create(result)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) error).getCode()).isEqualTo("NOTE_FORBIDDEN");
                })
                .verify();
    }
}
