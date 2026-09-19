package com.newtech.note.config;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.h2.H2Backend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Starts a disk-backed MongoDB-compatible server for the local debug profile.
 * No production profile loads this configuration.
 */
@Configuration
@Profile("local-dev")
public class LocalDevMongoConfiguration {

    @Bean(destroyMethod = "shutdown")
    MongoServer localDevMongoServer(
            @Value("${app.local-dev.mongodb.host:127.0.0.1}") String host,
            @Value("${app.local-dev.mongodb.port:27018}") int port,
            @Value("${app.local-dev.mongodb.file:local-data/note.mv}") String filename) throws java.io.IOException {
        java.nio.file.Path databaseFile = java.nio.file.Path.of(filename).toAbsolutePath().normalize();
        java.nio.file.Files.createDirectories(databaseFile.getParent());
        MongoServer server = new MongoServer(new H2Backend(databaseFile.toString()));
        server.bind(host, port);
        return server;
    }

    @Bean(destroyMethod = "close")
    MongoClient localDevMongoClient(
            MongoServer localDevMongoServer,
            @Value("${spring.data.mongodb.uri}") String mongoUri) {
        return MongoClients.create(mongoUri);
    }
}
