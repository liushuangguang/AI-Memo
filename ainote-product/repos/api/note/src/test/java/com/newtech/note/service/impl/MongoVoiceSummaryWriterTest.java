package com.newtech.note.service.impl;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MongoVoiceSummaryWriterTest {
    @Test
    void casQueryBindsOwnerActiveNoteModuleAndContentSnapshot() {
        Query query = MongoVoiceSummaryWriter.buildCasQuery(
                "note-1", "owner-1", "text-1", 2, "old-content", "save-request-1");
        Document bson = query.getQueryObject();

        assertEquals("note-1", bson.getString("_id"));
        assertEquals("owner-1", bson.getString("deviceId"));
        assertEquals(false, bson.getBoolean("deleted"));
        assertEquals(false,
                bson.get("voiceSummarySaveDigests.save-request-1", Document.class)
                        .getBoolean("$exists"));
        assertEquals("text-1", bson.getString("modules.2.moduleId"));
        assertEquals("old-content", bson.getString("modules.2.content"));
    }

    @Test
    void updateTouchesOnlyMatchedTextContentAndUpdatedAt() {
        LocalDateTime updatedAt = LocalDateTime.of(2026, 9, 6, 1, 2, 3);
        Update update = MongoVoiceSummaryWriter.buildContentUpdate(
                2, "new-content", "save-request-1", "summary-digest", updatedAt);
        Document set = update.getUpdateObject().get("$set", Document.class);

        assertEquals(3, set.size());
        assertEquals("new-content", set.getString("modules.2.content"));
        assertEquals(updatedAt, set.get("updatedAt"));
        assertEquals("summary-digest",
                set.getString("voiceSummarySaveDigests.save-request-1"));
    }
}
