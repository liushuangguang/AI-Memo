package com.newtech.note.service;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.common.NotePageData;
import com.newtech.note.entity.dto.NoteTheme;
import com.newtech.note.entity.request.CreateNoteThemeRequest;
import com.newtech.note.entity.request.SearchNoteThemeRequest;
import com.newtech.note.entity.request.UpdateNoteThemeRequest;
import reactor.core.publisher.Mono;

public interface NoteThemeService {
    Mono<NoteBaseResponse<NoteTheme>> createNoteTheme(String deviceId, CreateNoteThemeRequest request);

    Mono<NoteBaseResponse<NoteTheme>> updateNoteTheme(String deviceId, UpdateNoteThemeRequest request);

    Mono<NoteBaseResponse<NotePageData<NoteTheme>>> noteThemePagination(String deviceId, SearchNoteThemeRequest request);

    Mono<Long> count(String deviceId, SearchNoteThemeRequest request);

    Mono<NoteBaseResponse<NoteTheme>> deleteNoteTheme(String deviceId, String id);

    Mono<NoteTheme> findThemeById(String noteThemeId);
}
