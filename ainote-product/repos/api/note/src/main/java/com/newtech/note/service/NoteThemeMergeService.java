package com.newtech.note.service;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.entity.request.MergeNoteThemeRequest;
import com.newtech.note.entity.vo.NoteThemeCandidate;
import com.newtech.note.entity.vo.NoteThemeMergeResult;
import reactor.core.publisher.Mono;

import java.util.List;

public interface NoteThemeMergeService {
    Mono<NoteBaseResponse<List<NoteThemeCandidate>>> candidates(String deviceId, String themeId);

    Mono<NoteBaseResponse<NoteThemeMergeResult>> merge(
            String deviceId, String themeId, MergeNoteThemeRequest request);
}
