package com.newtech.note.config;

import com.mongodb.client.result.InsertOneResult;
import com.mongodb.reactivestreams.client.MongoClient;
import de.bwaldvogel.mongo.MongoServer;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LocalDevMongoConfigurationTest {
    @org.junit.jupiter.api.io.TempDir
    java.nio.file.Path temporary;

    @Test
    void startsDiskBackedMongoAndRetainsDataAcrossRestart() throws Exception {
        LocalDevMongoConfiguration configuration = new LocalDevMongoConfiguration();
        String file = temporary.resolve("note.mv").toString();
        MongoServer server = configuration.localDevMongoServer("127.0.0.1", 0, file);

        try (MongoClient client = configuration.localDevMongoClient(
                server, server.getConnectionString() + "/note")) {
            InsertOneResult result = Mono.from(client.getDatabase("note")
                            .getCollection("localDevSmokeTest")
                            .insertOne(new Document("status", "ok")))
                    .block(Duration.ofSeconds(5));

            assertNotNull(result);
            Document stored = Mono.from(client.getDatabase("note")
                            .getCollection("localDevSmokeTest")
                            .find(new Document("status", "ok"))
                            .first())
                    .block(Duration.ofSeconds(5));
            assertNotNull(stored);
            assertEquals("ok", stored.getString("status"));
        } finally {
            server.shutdown();
        }
        MongoServer restarted = configuration.localDevMongoServer("127.0.0.1", 0, file);
        try (MongoClient client = configuration.localDevMongoClient(restarted, restarted.getConnectionString()+"/note")) {
            Document stored = Mono.from(client.getDatabase("note").getCollection("localDevSmokeTest")
                    .find(new Document("status", "ok")).first()).block(Duration.ofSeconds(5));
            assertNotNull(stored);
            assertEquals("ok", stored.getString("status"));
        } finally { restarted.shutdown(); }
    }
}
