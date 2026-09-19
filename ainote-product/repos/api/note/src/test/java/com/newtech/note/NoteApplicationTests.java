package com.newtech.note;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.core.env.Environment;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest(properties = "jwt.key=test-offline-jwt-key-not-for-production-2026")
@ActiveProfiles("test-offline")
class NoteApplicationTests {

    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {
        Assertions.assertEquals("127.0.0.1", environment.getProperty("milvus.host"));
        Assertions.assertEquals("false", environment.getProperty("note.mongodb.indexes.enabled"));
    }

}
