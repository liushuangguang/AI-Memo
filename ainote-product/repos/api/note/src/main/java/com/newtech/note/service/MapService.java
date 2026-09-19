package com.newtech.note.service;

import com.newtech.note.entity.dto.map.NearByMerchants;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

public interface MapService {

    Mono<NearByMerchants> searchNearByMerchants(String address, String keyword, ServerHttpRequest req);
    Mono<String> queryProductKeywords(String input, ServerHttpRequest req);
}
