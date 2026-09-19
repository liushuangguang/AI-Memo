package com.newtech.note.repositories.impl;

import com.newtech.note.entity.dto.NoteAnalysisRecord;
import com.newtech.note.entity.request.UpdateNoteAnalysisRecordRequest;
import com.newtech.note.repositories.NoteAnalysisRecordDynamicRepository;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public class NoteAnalysisRecordDynamicRepositoryImpl implements NoteAnalysisRecordDynamicRepository {
    private final ReactiveMongoTemplate reactiveMongoTemplate;

    public NoteAnalysisRecordDynamicRepositoryImpl(ReactiveMongoTemplate reactiveMongoTemplate) {
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }

    public Mono<Integer> getLatestVersion(String noteId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("noteId").is(noteId)),
                Aggregation.group().max("version").as("maxVersion")
        );
        return reactiveMongoTemplate.aggregate(aggregation, "note_analysis_records", MaxVersionResult.class)
                .map(MaxVersionResult::maxVersion)
                .singleOrEmpty();
    }

    @Override
    public Mono<Integer> allocateNextVersion(String noteId, int knownLatestVersion) {
        Query query = Query.query(Criteria.where("_id").is(noteId));
        Update alignCounter = new Update().max("value", Math.max(0, knownLatestVersion));
        return reactiveMongoTemplate.upsert(query, alignCounter, "note_analysis_record_counters")
                .then(reactiveMongoTemplate.findAndModify(query,
                        new Update().inc("value", 1),
                        FindAndModifyOptions.options().returnNew(true),
                        VersionCounter.class,
                        "note_analysis_record_counters"))
                .map(VersionCounter::value)
                .switchIfEmpty(Mono.error(new IllegalStateException("Unable to allocate analysis version")));
    }

    @Override
    public Mono<NoteAnalysisRecord> patch(String id, UpdateNoteAnalysisRecordRequest request) {
        Update update = new Update().set("updatedAt", LocalDateTime.now());
        if (request.getOverallReview() != null) {
            update.set("overallReview", request.getOverallReview());
        }
        if (request.getOrganizedNote() != null) {
            update.set("organizedNote", request.getOrganizedNote());
        }
        if (request.getAiSuggestion() != null) {
            update.set("aiSuggestion", request.getAiSuggestion());
        }
        if (request.getRelatedTitle() != null) {
            update.set("relatedTitle", request.getRelatedTitle());
        }
        if (request.getRelatedLink() != null) {
            update.set("relatedLink", request.getRelatedLink());
        }
        if (request.getCategorizedNote() != null) {
            update.set("categorizedNotes", request.getCategorizedNote());
        }
        if (request.getRecommendedProducts() != null) {
            update.set("recommendedProducts", request.getRecommendedProducts());
        }
        if (request.getAiIllustration() != null) {
            update.set("aiIllustration", request.getAiIllustration());
        }
        if (request.getRelatedNoteIds() != null) {
            update.set("relatedNoteIds", request.getRelatedNoteIds());
        }
        return reactiveMongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(id)),
                update,
                FindAndModifyOptions.options().returnNew(true),
                NoteAnalysisRecord.class);
    }

    // 内部类用于映射聚合结果
    private record MaxVersionResult(Integer maxVersion) {
    }

    private record VersionCounter(String id, Integer value) {
    }
}
