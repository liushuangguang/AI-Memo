package com.newtech.note.controller;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.entity.request.voice.SaveVoiceDiscussionSummaryRequest;
import com.newtech.note.entity.request.voice.VoiceDiscussionRequest;
import com.newtech.note.entity.request.voice.VoiceDiscussionSummaryRequest;
import com.newtech.note.entity.vo.VoiceDiscussionReply;
import com.newtech.note.entity.vo.VoiceDiscussionSaveResult;
import com.newtech.note.entity.vo.VoiceDiscussionSummary;
import com.newtech.note.service.VoiceDiscussionServiceV2;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v2/voice-discussion")
@Tag(name = "语音讨论 V2", description = "有界笔记上下文的多轮讨论和用户确认后的总结写回")
public class VoiceDiscussionControllerV2 {
    private final VoiceDiscussionServiceV2 service;

    public VoiceDiscussionControllerV2(VoiceDiscussionServiceV2 service) {
        this.service = service;
    }

    @PostMapping("/respond")
    @Operation(summary = "继续一次语音或文本讨论")
    public Mono<NoteBaseResponse<VoiceDiscussionReply>> respond(
            @RequestBody VoiceDiscussionRequest request,
            ServerHttpRequest httpRequest) {
        return service.respond(request, httpRequest).map(NoteBaseResponse::success);
    }

    @PostMapping("/summary/preview")
    @Operation(summary = "生成总结预览，不修改笔记")
    public Mono<NoteBaseResponse<VoiceDiscussionSummary>> previewSummary(
            @RequestBody VoiceDiscussionSummaryRequest request,
            ServerHttpRequest httpRequest) {
        return service.previewSummary(request, httpRequest).map(NoteBaseResponse::success);
    }

    @PostMapping("/summary/save")
    @Operation(summary = "用户确认后将总结追加到当前笔记正文")
    public Mono<NoteBaseResponse<VoiceDiscussionSaveResult>> saveSummary(
            @RequestBody SaveVoiceDiscussionSummaryRequest request,
            ServerHttpRequest httpRequest) {
        return service.saveSummary(request, httpRequest).map(NoteBaseResponse::success);
    }
}
