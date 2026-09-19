package com.newtech.note.service;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.entity.vo.CategorizedNote;
import com.newtech.note.entity.dto.NoteAnalysis;
import com.newtech.note.entity.request.AnalyzeNoteRequest;
import com.newtech.note.entity.request.AssistContentRequest;
import com.newtech.note.entity.request.SelectAssistanceDirectionRequest;
import com.newtech.note.entity.vo.NoteProductRecommendation;
import com.newtech.note.entity.vo.NoteRelatedLink;
import com.newtech.note.entity.vo.RelationalInformationVo;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface NoteAnalysisService {

    Flux<String> organizedNoteText(AnalyzeNoteRequest request, ServerHttpRequest req);

    Mono<NoteBaseResponse<List<NoteAnalysis>>> relatedNotes(AnalyzeNoteRequest request, ServerHttpRequest req);

    Mono<NoteBaseResponse<List<CategorizedNote>>> categorizedNotes(AnalyzeNoteRequest request, ServerHttpRequest req);

    Mono<NoteBaseResponse<List<NoteProductRecommendation>>> productRecommendations(AnalyzeNoteRequest request, ServerHttpRequest req);

    Mono<NoteBaseResponse<List<NoteRelatedLink>>> relatedLinks(AnalyzeNoteRequest request, ServerHttpRequest req);

    Flux<String> assistedContent(AssistContentRequest request, ServerHttpRequest req);

    Mono<NoteBaseResponse<List<String>>> selectedAssistanceDirection(SelectAssistanceDirectionRequest request, ServerHttpRequest req);

    Mono<NoteBaseResponse<String>> imageLink(AnalyzeNoteRequest request, ServerHttpRequest req);

    Mono<NoteBaseResponse<RelationalInformationVo>> relationalInformation(AnalyzeNoteRequest request, ServerHttpRequest req);
}
