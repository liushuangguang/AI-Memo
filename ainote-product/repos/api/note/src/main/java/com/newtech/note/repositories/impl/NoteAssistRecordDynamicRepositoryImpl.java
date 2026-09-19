package com.newtech.note.repositories.impl;

import com.newtech.note.repositories.NoteAssistRecordDynamicRepository;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import reactor.core.publisher.Mono;

public class NoteAssistRecordDynamicRepositoryImpl implements NoteAssistRecordDynamicRepository {
    private final ReactiveMongoTemplate reactiveMongoTemplate;

    public NoteAssistRecordDynamicRepositoryImpl(ReactiveMongoTemplate reactiveMongoTemplate) {
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }

    public Mono<Integer> getLatestVersion(String noteId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("noteId").is(noteId)),
                Aggregation.group().max("version").as("maxVersion")
        );
        return reactiveMongoTemplate.aggregate(aggregation, "note_assist_records", MaxVersionResult.class)
                .map(MaxVersionResult::maxVersion)
                .singleOrEmpty();
    }

    // 内部类用于映射聚合结果
    private record MaxVersionResult(Integer maxVersion) {
    }
}
