package com.newtech.note.service;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.common.NotePageData;
import com.newtech.note.entity.dto.Note;
import com.newtech.note.entity.request.CreateNoteRequest;
import com.newtech.note.entity.request.SearchNoteRequest;
import com.newtech.note.entity.request.UpdateNoteRequest;
import com.newtech.note.entity.request.noteModule.*;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

public interface NoteServiceV2 {
    Mono<NoteBaseResponse<Note>> createNote(String deviceId, CreateNoteRequest request);

    Mono<NoteBaseResponse<Note>> createImageNote(String deviceId, FilePart filePart);

    Mono<NoteBaseResponse<Note>> createImageNote(String deviceId, FilePart filePart, String recognizedText);

    Mono<NoteBaseResponse<Note>> createImageNote(
            String deviceId, FilePart filePart, String recognizedText, String requestId);

    Mono<Note> getNoteById(String id, String deviceId);

    Mono<Note> findNoteById(String id);

    Flux<Note> listNote(String deviceId, SearchNoteRequest request);

    /** Lists only active user-dimension notes belonging to one authenticated owner. */
    Flux<Note> listRelatedNoteCandidates(String ownerId, String excludedNoteId);

    Mono<NoteBaseResponse<NotePageData<Note>>> notePagination(String deviceId, SearchNoteRequest request);

    Mono<NoteBaseResponse<Note>> updateNote(String id, UpdateNoteRequest request);

    Mono<NoteBaseResponse<Note>> deleteNote(String deviceId, String id);

    Mono<Long> count(String deviceId, SearchNoteRequest searchNoteRequest);

    Flux<Note> getNotesByIds(Set<String> ids);

    Flux<Note> getNotesByIds(List<String> ids);

    Mono<NoteBaseResponse<Note>> addNoteModule(String deviceId, AddNoteModuleRequest request);

    Mono<NoteBaseResponse<Note>> updateNoteModule(String deviceId, UpdateNoteModuleRequest request);

    Mono<NoteBaseResponse<Note>> deleteNoteModule(String deviceId, DeleteNoteModuleRequest request);

    Mono<NoteBaseResponse<Note>> moveNoteModule(String deviceId, MoveNoteModuleRequest request);

    Mono<NoteBaseResponse<Note>> addNoteModuleItem(String deviceId, AddNoteModuleItemRequest request);

    Mono<NoteBaseResponse<Note>> updateNoteModuleItem(String deviceId, UpdateNoteModuleItemRequest request);

    Mono<NoteBaseResponse<Note>> deleteNoteModuleItem(String deviceId, DeleteNoteModuleItemRequest request);

    Mono<NoteBaseResponse<Note>> replaceTextModule(String deviceId, ReplaceTextModuleRequest request);

    Mono<List<Note>> addNotesThemeId(List<String> noteIds, String themeId);
}
