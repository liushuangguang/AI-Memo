package com.newtech.note.client;

import reactor.core.publisher.Mono;

public interface RecognizedTextOrganizer {
    Mono<OrganizedText> organize(String recognizedText);

    record OrganizedText(String title, String content) {
    }
}
