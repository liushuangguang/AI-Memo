package com.newtech.note.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.newtech.note.entity.dto.NoteAnalysisHistory;
import com.newtech.note.entity.request.DeleteNoteDiscussAudioRequest;
import com.newtech.note.entity.request.GetNoteDiscussAudioRequest;
import com.newtech.note.entity.request.OrganizeToDoListNoteRequest;
import com.newtech.note.entity.request.UpdateNoteDiscussAudioRequest;
import reactor.core.publisher.Mono;

public interface AudioSessionService {

    Mono<String> getOrganizeToDoListNote(OrganizeToDoListNoteRequest request);

    Mono<JsonNode> getNoteDiscussAudio(GetNoteDiscussAudioRequest request);

    Mono<NoteAnalysisHistory> updateNoteDiscussAudio(UpdateNoteDiscussAudioRequest request);

    Mono<Integer> deleteNoteDiscussAudio(DeleteNoteDiscussAudioRequest request);

    Mono<String> noteDiscussAudioAiSummary(JsonNode talkAudio);
}
