package com.newtech.note.service;

import com.newtech.note.entity.dto.NoteAssistRecord;
import com.newtech.note.entity.request.CreateNoteAssistRecordRequest;
import com.newtech.note.entity.request.UpdateNoteAssistRecordRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NoteAssistRecordService {
    Mono<NoteAssistRecord> findById(String recordId);

    Mono<NoteAssistRecord> update(UpdateNoteAssistRecordRequest request);

    Mono<NoteAssistRecord> create(CreateNoteAssistRecordRequest request);


    Mono<NoteAssistRecord> latestAssist(String noteId);

    Mono<NoteAssistRecord> specificAssist(String noteId, int version);

    Flux<NoteAssistRecord> allAssistRecords(String noteId);
}
