package com.newtech.note.service;

import com.newtech.note.entity.request.voice.SaveVoiceDiscussionSummaryRequest;
import com.newtech.note.entity.request.voice.VoiceDiscussionRequest;
import com.newtech.note.entity.request.voice.VoiceDiscussionSummaryRequest;
import com.newtech.note.entity.vo.VoiceDiscussionReply;
import com.newtech.note.entity.vo.VoiceDiscussionSaveResult;
import com.newtech.note.entity.vo.VoiceDiscussionSummary;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

public interface VoiceDiscussionServiceV2 {
    Mono<VoiceDiscussionReply> respond(VoiceDiscussionRequest request, ServerHttpRequest httpRequest);

    Mono<VoiceDiscussionSummary> previewSummary(VoiceDiscussionSummaryRequest request,
                                                ServerHttpRequest httpRequest);

    Mono<VoiceDiscussionSaveResult> saveSummary(SaveVoiceDiscussionSummaryRequest request,
                                                ServerHttpRequest httpRequest);
}
