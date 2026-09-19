package com.newtech.note.service;

import com.newtech.note.entity.dto.*;
import com.newtech.note.entity.dto.noteRelatedInfo.RelatedTitle;
import com.newtech.note.entity.search.WebPage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface DifyNoteService {
    Mono<String> isNoteMakeSense(String input, ServerHttpRequest request);

    Mono<OrganizedNoteDto> organizeNote(String input, ServerHttpRequest request);

    Mono<List<WebPage>> relatedLink(String input, ServerHttpRequest request);

    Mono<List<RelatedTitle>> relatedTitle(String input, ServerHttpRequest request);

    Mono<String> relatedInfo(String input, ServerHttpRequest request);

    Flux<String> aiSuggestion(String input, ServerHttpRequest request);

    Mono<InfoClassification> infoClassification(String input, ServerHttpRequest request);

    Mono<RewrittenContentDto> rewriteContent(String note, String contentToBeModified, String rewriteRequirement, ServerHttpRequest request);

    Mono<ContentAssistance> contentAssistance(String input, ServerHttpRequest request);

    Mono<List<CompleteInfo>> completeInfo(String input, ServerHttpRequest request);
}
