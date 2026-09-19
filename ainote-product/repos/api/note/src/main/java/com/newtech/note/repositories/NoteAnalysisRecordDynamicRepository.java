package com.newtech.note.repositories;

import com.newtech.note.entity.dto.NoteAnalysisRecord;
import com.newtech.note.entity.request.UpdateNoteAnalysisRecordRequest;
import reactor.core.publisher.Mono;

public interface NoteAnalysisRecordDynamicRepository {

    Mono<Integer> getLatestVersion(String noteId);

    Mono<Integer> allocateNextVersion(String noteId, int knownLatestVersion);

    Mono<NoteAnalysisRecord> patch(String id, UpdateNoteAnalysisRecordRequest request);
}
