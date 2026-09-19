package com.newtech.note.service;

import com.newtech.note.entity.dto.NoteAnalysisRecord;
import com.newtech.note.entity.request.CreateNoteAnalysisRecordRequest;
import com.newtech.note.entity.request.UpdateNoteAnalysisRecordRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NoteAnalysisRecordService {
    Mono<NoteAnalysisRecord> create(CreateNoteAnalysisRecordRequest request);

    Mono<NoteAnalysisRecord> findOrCreateForSnapshot(String noteId, String rawNote);

    Mono<NoteAnalysisRecord> update(UpdateNoteAnalysisRecordRequest request);

    Mono<NoteAnalysisRecord> latestAnalysis(String noteId);

    Mono<NoteAnalysisRecord> findById(String id);

    Flux<NoteAnalysisRecord> allAnalysisRecords(String noteId);

    Mono<NoteAnalysisRecord> specificAnalysis(String noteAnalysisId, int version);


}
