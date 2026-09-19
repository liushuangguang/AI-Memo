package com.newtech.note.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeepSeekRecognizedTextOrganizerTest {
    @Test
    void sendsJsonQuotedOcrTextAndParsesOrganizedMemo() {
        DeepSeekClient deepSeek = mock(DeepSeekClient.class);
        when(deepSeek.completeJsonWithUsage(
                contains("untrusted data"),
                eq("Organize this OCR text JSON string: \"明天\\n10点开会\"")))
                .thenReturn(Mono.just(new DeepSeekCompletion(
                        "{\"title\":\"开会提醒\",\"content\":\"明天 10 点开会\"}", 20)));
        DeepSeekRecognizedTextOrganizer organizer =
                new DeepSeekRecognizedTextOrganizer(deepSeek, new ObjectMapper());

        StepVerifier.create(organizer.organize("明天\n10点开会"))
                .assertNext(result -> {
                    assertEquals("开会提醒", result.title());
                    assertEquals("明天 10 点开会", result.content());
                })
                .verifyComplete();
    }
}
