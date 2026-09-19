package com.newtech.note.service.impl;

import com.newtech.note.entity.dto.Note;
import com.newtech.note.service.VoiceSummaryWriter;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/** Performs the voice-summary write without replacing the surrounding modules array. */
@Component
public class MongoVoiceSummaryWriter implements VoiceSummaryWriter {
    private final ReactiveMongoTemplate mongoTemplate;

    public MongoVoiceSummaryWriter(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Note> compareAndSetTextContent(String noteId,
                                               String ownerId,
                                               String moduleId,
                                               int moduleIndex,
                                               String expectedContent,
                                               String newContent,
                                               String saveRequestId,
                                               String summaryDigest) {
        Query query = buildCasQuery(
                noteId, ownerId, moduleId, moduleIndex, expectedContent, saveRequestId);
        Update update = buildContentUpdate(
                moduleIndex, newContent, saveRequestId, summaryDigest, LocalDateTime.now());
        return mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Note.class);
    }

    @Override
    public Mono<Note> findOwnedActiveNote(String noteId, String ownerId) {
        return mongoTemplate.findOne(buildOwnedActiveQuery(noteId, ownerId), Note.class);
    }

    static Query buildCasQuery(String noteId,
                               String ownerId,
                               String moduleId,
                               int moduleIndex,
                               String expectedContent,
                               String saveRequestId) {
        String modulePath = modulePath(moduleIndex);
        return buildOwnedActiveQuery(noteId, ownerId)
                .addCriteria(Criteria.where(saveDigestPath(saveRequestId)).exists(false))
                .addCriteria(Criteria.where(modulePath + ".moduleId").is(moduleId))
                .addCriteria(Criteria.where(modulePath + ".content").is(expectedContent));
    }

    static Query buildOwnedActiveQuery(String noteId, String ownerId) {
        return Query.query(Criteria.where("_id").is(noteId)
                .and("deviceId").is(ownerId)
                .and("deleted").is(false));
    }

    static Update buildContentUpdate(int moduleIndex,
                                     String newContent,
                                     String saveRequestId,
                                     String summaryDigest,
                                     LocalDateTime updatedAt) {
        return new Update()
                .set(modulePath(moduleIndex) + ".content", newContent)
                .set("updatedAt", updatedAt)
                .set(saveDigestPath(saveRequestId), summaryDigest);
    }

    private static String saveDigestPath(String saveRequestId) {
        return "voiceSummarySaveDigests." + saveRequestId;
    }

    private static String modulePath(int moduleIndex) {
        if (moduleIndex < 0) {
            throw new IllegalArgumentException("moduleIndex must be non-negative");
        }
        return "modules." + moduleIndex;
    }
}
