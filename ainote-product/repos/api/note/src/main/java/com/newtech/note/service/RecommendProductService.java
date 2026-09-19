package com.newtech.note.service;

import com.newtech.note.entity.dto.product.Product;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface RecommendProductService {
    Mono<List<Product>> searchProduct(String query, ServerHttpRequest req);
    Mono<List<List<String>>> queryProductKeywords(String query, ServerHttpRequest req);
}
