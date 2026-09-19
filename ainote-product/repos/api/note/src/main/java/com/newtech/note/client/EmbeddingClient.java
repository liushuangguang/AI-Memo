package com.newtech.note.client;

import com.newtech.note.client.entity.embedding.InsertNoteReq;
import com.newtech.note.client.entity.embedding.InsertNoteRes;
import com.newtech.note.client.entity.embedding.UpsertThemeReq;
import com.newtech.note.client.entity.embedding.UpsertThemeRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class EmbeddingClient {
    private final WebClient webClient;

    @Value("${dify.server:http://150.158.11.172}")
    private static final String EMBEDDING_SERVER_URL = "https://150.158.11.172:6000";

    public EmbeddingClient(WebClient webClient) {
        this.webClient = webClient;
    }

    // Update Theme
    public Mono<UpsertThemeRes> upsertTheme(UpsertThemeReq request) {
        return webClient.put()
                .uri(EMBEDDING_SERVER_URL + "/theme/upsert")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UpsertThemeRes.class)
                .doOnError(e -> log.error("Error upsetting theme: {}", e.getMessage()));
    }

    // Insert Note
    public Mono<InsertNoteRes> insertNote(InsertNoteReq request) {
        return webClient.post()
                .uri(EMBEDDING_SERVER_URL + "/insert")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(InsertNoteRes.class)
                .doOnError(e -> log.error("Error inserting note: {}", e.getMessage()));
    }
}

