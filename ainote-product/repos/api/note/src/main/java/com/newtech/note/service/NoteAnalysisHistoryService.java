package com.newtech.note.service;

import com.newtech.note.entity.dto.NoteAnalysisHistory;
import com.newtech.note.entity.request.CreateNoteAnalysisHistoryRequest;
import com.newtech.note.entity.request.UpdateNoteAnalysisHistoryRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NoteAnalysisHistoryService {
    Mono<NoteAnalysisHistory> create(CreateNoteAnalysisHistoryRequest request);

    Mono<NoteAnalysisHistory> update(UpdateNoteAnalysisHistoryRequest request);

    Mono<NoteAnalysisHistory> latestAnalysis(long noteAnalysisId, int analysisType);

    Mono<NoteAnalysisHistory> findByNoteAnalysisIdAndVersion(long noteAnalysisId, int version, int analysisType);

    Flux<NoteAnalysisHistory> allAnalysisHistories(long noteAnalysisId);

    Mono<NoteAnalysisHistory> specificAnalysis(long noteAnalysisId, int version, int analysisType);
}
