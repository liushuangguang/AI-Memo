package com.newtech.note.security;

import com.newtech.note.common.BusinessException;
import com.newtech.note.entity.dto.Note;
import com.newtech.note.entity.dto.NoteAnalysisRecord;
import com.newtech.note.entity.dto.NoteAssistRecord;
import com.newtech.note.exception.NoteGlobalExceptionHandler;
import com.newtech.note.repositories.NoteAnalysisRecordRepository;
import com.newtech.note.repositories.NoteAssistRecordRepository;
import com.newtech.note.repositories.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class NoteOwnershipServiceTest {
    private RequestIdentityService identityService;
    private NoteRepository noteRepository;
    private NoteAnalysisRecordRepository analysisRepository;
    private NoteAssistRecordRepository assistRepository;
    private NoteOwnershipService service;

    @BeforeEach
    void setUp() {
        identityService = mock(RequestIdentityService.class);
        noteRepository = mock(NoteRepository.class);
        analysisRepository = mock(NoteAnalysisRecordRepository.class);
        assistRepository = mock(NoteAssistRecordRepository.class);
        service = new NoteOwnershipService(identityService, noteRepository, analysisRepository, assistRepository);
    }

    @Test
    void unauthenticatedRequestFailsBeforeAnyRecordOrNoteLookup() {
        ServerHttpRequest request = request("none");
        when(identityService.resolve(request)).thenReturn(Mono.error(
                new BusinessException("UNAUTHORIZED", "unauthorized")));

        assertCode(service.ownedAnalysisRecord("record", request), "UNAUTHORIZED");

        verifyNoInteractions(noteRepository, analysisRepository, assistRepository);
    }

    @Test
    void mobileBobCannotReadAliceAnalysisRecord() {
        ServerHttpRequest request = request("bob");
        when(identityService.resolve(request)).thenReturn(Mono.just(identity("2", 2L, false)));
        NoteAnalysisRecord record = new NoteAnalysisRecord();
        record.setId("record");
        record.setNoteId("alice-note");
        when(analysisRepository.findById("record")).thenReturn(Mono.just(record));
        when(noteRepository.findById("alice-note")).thenReturn(Mono.just(note("alice-note", "1")));

        assertCode(service.ownedAnalysisRecord("record", request), "ANALYSIS_RECORD_NOT_FOUND");
        verifyNoInteractions(assistRepository);
    }

    @Test
    void guestBHasNoAccessToGuestANote() {
        ServerHttpRequest request = request("guest-b");
        when(identityService.resolve(request)).thenReturn(Mono.just(identity("guest-b", null, true)));
        when(noteRepository.findById("guest-note")).thenReturn(Mono.just(note("guest-note", "guest-a")));

        assertCode(service.ownedNote("guest-note", request), "NOTE_FORBIDDEN");
        verifyNoInteractions(analysisRepository, assistRepository);
    }

    @Test
    void ownerCanReadItsRecordAndUnknownRecordDoesNotProbeNotes() {
        ServerHttpRequest request = request("alice");
        when(identityService.resolve(request)).thenReturn(Mono.just(identity("1", 1L, false)));
        NoteAnalysisRecord record = new NoteAnalysisRecord();
        record.setId("record");
        record.setNoteId("alice-note");
        when(analysisRepository.findById("record")).thenReturn(Mono.just(record));
        when(analysisRepository.findById("missing")).thenReturn(Mono.empty());
        when(noteRepository.findById("alice-note")).thenReturn(Mono.just(note("alice-note", "1")));

        StepVerifier.create(service.ownedAnalysisRecord("record", request))
                .expectNext(record)
                .verifyComplete();
        clearInvocations(noteRepository);
        assertCode(service.ownedAnalysisRecord("missing", request), "ANALYSIS_RECORD_NOT_FOUND");
        verifyNoInteractions(noteRepository);
    }

    @Test
    void missingAndForeignRecordsHaveIdenticalExternalErrors() {
        ServerHttpRequest request = request("bob");
        when(identityService.resolve(request)).thenReturn(Mono.just(identity("2", 2L, false)));
        NoteAnalysisRecord foreignAnalysis = new NoteAnalysisRecord();
        foreignAnalysis.setId("foreign-analysis");
        foreignAnalysis.setNoteId("alice-note");
        NoteAssistRecord foreignAssist = new NoteAssistRecord();
        foreignAssist.setId("foreign-assist");
        foreignAssist.setNoteId("alice-note");
        when(analysisRepository.findById("foreign-analysis")).thenReturn(Mono.just(foreignAnalysis));
        when(analysisRepository.findById("missing-analysis")).thenReturn(Mono.empty());
        when(assistRepository.findById("foreign-assist")).thenReturn(Mono.just(foreignAssist));
        when(assistRepository.findById("missing-assist")).thenReturn(Mono.empty());
        when(noteRepository.findById("alice-note")).thenReturn(Mono.just(note("alice-note", "1")));

        BusinessException analysisForeign = capture(service.ownedAnalysisRecord("foreign-analysis", request));
        BusinessException analysisMissing = capture(service.ownedAnalysisRecord("missing-analysis", request));
        assertThat(analysisForeign.getCode()).isEqualTo(analysisMissing.getCode())
                .isEqualTo("ANALYSIS_RECORD_NOT_FOUND");
        assertThat(analysisForeign.getMessage()).isEqualTo(analysisMissing.getMessage());

        BusinessException assistForeign = capture(service.ownedAssistRecord("foreign-assist", request));
        BusinessException assistMissing = capture(service.ownedAssistRecord("missing-assist", request));
        assertThat(assistForeign.getCode()).isEqualTo(assistMissing.getCode())
                .isEqualTo("ASSIST_RECORD_NOT_FOUND");
        assertThat(assistForeign.getMessage()).isEqualTo(assistMissing.getMessage());

        NoteGlobalExceptionHandler handler = new NoteGlobalExceptionHandler();
        var foreignResponse = handler.handleBusinessException(analysisForeign).block();
        var missingResponse = handler.handleBusinessException(analysisMissing).block();
        assertThat(foreignResponse).isNotNull();
        assertThat(missingResponse).isNotNull();
        assertThat(foreignResponse.getStatusCode()).isEqualTo(missingResponse.getStatusCode());
        assertThat(foreignResponse.getBody()).isEqualTo(missingResponse.getBody());
        assertThat(foreignResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(foreignResponse.getBody()).isEqualTo("Analysis record was not found");
    }

    private RequestIdentityService.RequestIdentity identity(String owner, Long uid, boolean guest) {
        return new RequestIdentityService.RequestIdentity(owner, uid, null, null, guest);
    }

    private Note note(String id, String owner) {
        Note note = new Note();
        note.setId(id);
        note.setDeviceId(owner);
        return note;
    }

    private ServerHttpRequest request(String marker) {
        return MockServerHttpRequest.get("/test").header("X-Test", marker).build();
    }

    private void assertCode(Mono<?> result, String code) {
        StepVerifier.create(result)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) error).getCode()).isEqualTo(code);
                })
                .verify();
    }

    private BusinessException capture(Mono<?> result) {
        try {
            result.block();
            throw new AssertionError("Expected BusinessException");
        } catch (BusinessException failure) {
            return failure;
        }
    }
}
