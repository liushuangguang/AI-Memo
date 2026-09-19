package com.newtech.note.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtech.note.client.AiProviderOutputException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RecommendProductServiceImplTest {
    private final RecommendProductServiceImpl service =
            new RecommendProductServiceImpl(new ObjectMapper(), null, null, null, null);
    @Test void parsesOnlyBoundedSearchPhrases() {
        assertEquals(List.of(List.of("猫砂盆")), service.parseQueries("{\"queries\":[\"猫砂盆\"]}"));
        assertEquals(List.of(), service.parseQueries("{\"queries\":[]}"));
        assertThrows(AiProviderOutputException.class, () -> service.parseQueries("{\"queries\":[2]}"));
        assertThrows(AiProviderOutputException.class, () -> service.parseQueries("{\"queries\":[\"a\",\"b\",\"c\",\"d\"]}"));
        assertThrows(AiProviderOutputException.class, () -> service.parseQueries("{\"queries\":[\"" + "x".repeat(81) + "\"]}"));
    }
    @Test void rejectsUnsafeLinks() {
        assertTrue(RecommendProductServiceImpl.safeUrl("https://item.jd.com/123.html"));
        for (String value : List.of("javascript:alert(1)", "file:///a", "https://127.0.0.1/a",
                "https://user:password@example.com", "http://example.com", "https://device.local")) {
            assertFalse(RecommendProductServiceImpl.safeUrl(value));
        }
        assertFalse(RecommendProductServiceImpl.safeUrl(null));
    }
}
