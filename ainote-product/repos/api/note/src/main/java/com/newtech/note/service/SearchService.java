package com.newtech.note.service;

import com.newtech.note.entity.search.WebPage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface SearchService {
    Mono<List<WebPage>> search(String query, ServerHttpRequest req);
    Mono<List<String>> searchKeywords(String query, ServerHttpRequest req);
}
