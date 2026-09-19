package com.newtech.note.service;

import io.milvus.grpc.IDs;
import reactor.core.publisher.Mono;

public interface NoteEmbeddingService {
    Mono<IDs> embed(String noteId);

}
