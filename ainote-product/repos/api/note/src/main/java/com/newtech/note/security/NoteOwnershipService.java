package com.newtech.note.security;

import com.newtech.note.common.BusinessException;
import com.newtech.note.entity.dto.Note;
import com.newtech.note.entity.dto.NoteAnalysisRecord;
import com.newtech.note.entity.dto.NoteAssistRecord;
import com.newtech.note.repositories.NoteAnalysisRecordRepository;
import com.newtech.note.repositories.NoteAssistRecordRepository;
import com.newtech.note.repositories.NoteRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Ownership guard shared by active v2 analysis, assist, and record routes. */
@Component
public class NoteOwnershipService {
    private final RequestIdentityService identityService;
    private final NoteRepository noteRepository;
    private final NoteAnalysisRecordRepository analysisRecordRepository;
    private final NoteAssistRecordRepository assistRecordRepository;

    public NoteOwnershipService(RequestIdentityService identityService,
                                NoteRepository noteRepository,
                                NoteAnalysisRecordRepository analysisRecordRepository,
                                NoteAssistRecordRepository assistRecordRepository) {
        this.identityService = identityService;
        this.noteRepository = noteRepository;
        this.analysisRecordRepository = analysisRecordRepository;
        this.assistRecordRepository = assistRecordRepository;
    }

    public Mono<RequestIdentityService.RequestIdentity> authenticated(ServerHttpRequest request) {
        return identityService.resolve(request);
    }

    public Mono<Note> ownedNote(String noteId, ServerHttpRequest request) {
        if (StringUtils.isBlank(noteId)) {
            return Mono.error(new BusinessException("NOTE_ID_REQUIRED", "Note id is required"));
        }
        return identityService.resolve(request)
                .flatMap(identity -> ownedNote(noteId, identity));
    }

    public Mono<NoteAnalysisRecord> ownedAnalysisRecord(String recordId, ServerHttpRequest request) {
        if (StringUtils.isBlank(recordId)) {
            return Mono.error(new BusinessException("ANALYSIS_RECORD_ID_REQUIRED",
                    "Analysis record id is required"));
        }
        return identityService.resolve(request)
                .flatMap(identity -> analysisRecordRepository.findById(recordId)
                        .switchIfEmpty(analysisRecordNotFound())
                        .flatMap(record -> ownedNote(record.getNoteId(), identity)
                                .thenReturn(record)
                                .onErrorResume(BusinessException.class,
                                        ignored -> analysisRecordNotFound())));
    }

    public Mono<NoteAssistRecord> ownedAssistRecord(String recordId, ServerHttpRequest request) {
        if (StringUtils.isBlank(recordId)) {
            return Mono.error(new BusinessException("ASSIST_RECORD_ID_REQUIRED", "Assist record id is required"));
        }
        return identityService.resolve(request)
                .flatMap(identity -> assistRecordRepository.findById(recordId)
                        .switchIfEmpty(assistRecordNotFound())
                        .flatMap(record -> ownedNote(record.getNoteId(), identity)
                                .thenReturn(record)
                                .onErrorResume(BusinessException.class,
                                        ignored -> assistRecordNotFound())));
    }

    private <T> Mono<T> analysisRecordNotFound() {
        return Mono.error(new BusinessException("ANALYSIS_RECORD_NOT_FOUND",
                "Analysis record was not found"));
    }

    private <T> Mono<T> assistRecordNotFound() {
        return Mono.error(new BusinessException("ASSIST_RECORD_NOT_FOUND",
                "Assist record was not found"));
    }

    private Mono<Note> ownedNote(String noteId, RequestIdentityService.RequestIdentity identity) {
        return noteRepository.findById(noteId)
                .switchIfEmpty(Mono.error(new BusinessException("NOTE_NOT_FOUND", "Note was not found")))
                .flatMap(note -> Objects.equals(identity.ownerId(), note.getDeviceId())
                        ? Mono.just(note)
                        : Mono.error(new BusinessException("NOTE_FORBIDDEN",
                        "The authenticated user does not own this note")));
    }
}
