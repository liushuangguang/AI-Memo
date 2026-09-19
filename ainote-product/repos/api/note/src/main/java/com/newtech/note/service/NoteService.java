package com.newtech.note.service;

import com.newtech.note.entity.dto.NoteAnalysis;
import com.newtech.note.entity.request.CreateNoteAnalysisRequest;
import com.newtech.note.entity.request.SearchNoteRequest;
import com.newtech.note.entity.request.UpdateNoteAnalysisRequest;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface NoteService {
    Mono<NoteAnalysis> createNote(String deviceId, CreateNoteAnalysisRequest request);

    Mono<NoteAnalysis> createImageNote(String deviceId, FilePart filePart);

    Mono<NoteAnalysis> appendToOriginalText(long id, String toAppendText);

    Mono<NoteAnalysis> prependToOriginalText(long id, String toAppendText);

    Mono<NoteAnalysis> getNoteById(String deviceId, long id);

    Flux<NoteAnalysis> listNote(String deviceId, SearchNoteRequest request);

    Mono<NoteAnalysis> updateNote(long id, UpdateNoteAnalysisRequest request);

    Mono<NoteAnalysis> deleteNote(long id);

    Mono<Long> count(String deviceId, SearchNoteRequest searchNoteRequest);

    Flux<NoteAnalysis> getNotesByIds(List<Long> ids);
}
