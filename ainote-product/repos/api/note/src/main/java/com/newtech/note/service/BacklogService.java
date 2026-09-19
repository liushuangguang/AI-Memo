package com.newtech.note.service;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.common.NotePageData;
import com.newtech.note.entity.dto.Backlog;
import com.newtech.note.entity.request.CreateBacklogRequest;
import com.newtech.note.entity.request.SearchBacklogRequest;
import com.newtech.note.entity.request.UpdateBacklogRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BacklogService {
    Mono<NoteBaseResponse<Backlog>> createBacklog(String deviceId, CreateBacklogRequest request);

    Mono<Backlog> getBacklogById(String id, String deviceId);

    Flux<Backlog> listBacklog(String deviceId, SearchBacklogRequest request);

    Mono<NoteBaseResponse<NotePageData<Backlog>>> backlogPagination(String deviceId, SearchBacklogRequest request);

    Mono<NoteBaseResponse<Backlog>> updateBacklog(String id, UpdateBacklogRequest request);

    Mono<NoteBaseResponse<Backlog>> deleteBacklog(String deviceId, String id);

    Mono<Long> count(String deviceId, SearchBacklogRequest searchBacklogRequest);
}
