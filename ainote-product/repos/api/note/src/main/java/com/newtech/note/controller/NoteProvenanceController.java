package com.newtech.note.controller;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.entity.dto.NoteTheme;
import com.newtech.note.entity.dto.NoteThemeMergeHistory;
import com.newtech.note.security.NoteOwnershipService;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Objects;

/** Read-only provenance for the opened note, never an unscoped theme search. */
@RestController
@RequestMapping("/v2/note")
public class NoteProvenanceController {
    private final NoteOwnershipService ownership;
    private final ReactiveMongoTemplate mongo;
    public NoteProvenanceController(NoteOwnershipService ownership, ReactiveMongoTemplate mongo) {
        this.ownership = ownership;
        this.mongo = mongo;
    }

    @GetMapping("/{id}/merge-history")
    public Mono<NoteBaseResponse<List<NoteThemeMergeHistory>>> history(
            @PathVariable String id, ServerHttpRequest request) {
        return ownership.ownedNote(id, request).flatMap(note -> {
            if (note.isDeleted() || note.getNoteThemeIds() == null || note.getNoteThemeIds().isEmpty()) {
                return Mono.just(NoteBaseResponse.success(List.<NoteThemeMergeHistory>of()));
            }
            var query = Query.query(Criteria.where("_id").in(note.getNoteThemeIds())
                    .and("deviceId").is(note.getDeviceId()).and("deleted").ne(true)
                    .and("mergeHistory.mergedNoteId").is(id)).limit(20);
            return mongo.find(query, NoteTheme.class)
                    .flatMapIterable(theme -> theme.getMergeHistory() == null ? List.<NoteThemeMergeHistory>of() : theme.getMergeHistory())
                    .filter(history -> Objects.equals(id, history.getMergedNoteId()))
                    .take(20).collectList().map(NoteBaseResponse::success);
        });
    }
}
