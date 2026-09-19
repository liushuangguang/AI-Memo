package com.newtech.note.service;

import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

public interface ImageGenService {
    String DEGRADED_URL_MARKER = "/ai-illustration-fallback-";

    static boolean isDegradedResult(String imageUrl) {
        return imageUrl != null && imageUrl.contains(DEGRADED_URL_MARKER);
    }

    /**
     * 生成图片
     * @param input 输入文本
     * @return 图片路径
     */
    Mono<String> gen(String input, ServerHttpRequest req);
}
