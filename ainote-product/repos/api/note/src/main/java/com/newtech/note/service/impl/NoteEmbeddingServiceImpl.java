package com.newtech.note.service.impl;

import com.newtech.note.client.SiliconFlowEmbeddingClient;
import com.newtech.note.entity.dto.Note;
import com.newtech.note.service.MilvusService;
import com.newtech.note.service.NoteEmbeddingService;
import com.newtech.note.service.NoteServiceV2;
import io.milvus.grpc.IDs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.newtech.note.Constant.NOTE_EMBEDDING_TABLE_NAME;

@Slf4j
@Service
public class NoteEmbeddingServiceImpl implements NoteEmbeddingService {
    private final SiliconFlowEmbeddingClient siliconFlowEmbeddingClient;
    private final NoteServiceV2 noteService;
    private final MilvusService milvusService;

    public NoteEmbeddingServiceImpl(SiliconFlowEmbeddingClient siliconFlowEmbeddingClient, NoteServiceV2 noteService, MilvusService milvusService) {
        this.siliconFlowEmbeddingClient = siliconFlowEmbeddingClient;
        this.noteService = noteService;
        this.milvusService = milvusService;
    }

    @Override
    public Mono<IDs> embed(String noteId) {
        return noteService.getNotesByIds(List.of(noteId)).collectList()
                .flatMap(notes -> {
                    Note note = notes.getFirst();
                    return siliconFlowEmbeddingClient.getEmbedding(note.getContent())
                            .flatMap(embeddingResponse -> {
                                List<SiliconFlowEmbeddingClient.EmbeddingResponse.Data> embeddingDataList = embeddingResponse.data();
                                if (CollectionUtils.isEmpty(embeddingDataList)) {
                                    return Mono.empty();
                                }
                                List<Float> embedding = embeddingDataList.getFirst().embedding();
                                return milvusService.upsertData(NOTE_EMBEDDING_TABLE_NAME, note.getDeviceId(), note.getId(), note.getContent(), "", embedding)
                                        .map(mutationResult -> {
                                            if (mutationResult.getStatus().getCode() == 0) {
                                                log.info("Successfully upserted note {} embedding to milvus", noteId);
                                            }
                                            return mutationResult.getIDs();
                                        });
                            });
                });
    }


}
