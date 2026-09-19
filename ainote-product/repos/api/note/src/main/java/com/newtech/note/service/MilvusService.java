package com.newtech.note.service;

import io.milvus.grpc.MutationResult;
import reactor.core.publisher.Mono;

import java.util.List;

public interface MilvusService {
    Mono<MutationResult> insertData(String collectionName, String uid, String noteId, String note, String noteThemeId, List<Float> vector);

    Mono<MutationResult> upsertData(String collectionName, String uid, String noteId, String note, String noteThemeId, List<Float> vector);

    // 异步查询数据
    Mono<List<String>> searchData(String collectionName, List<Float> queryVectors, int topK);
}
