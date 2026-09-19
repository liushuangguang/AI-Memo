package com.newtech.note.service.impl;

import com.mongodb.client.result.UpdateResult;
import com.newtech.note.client.EmbeddingClient;
import com.newtech.note.entity.dto.NoteTheme;
import com.newtech.note.entity.dto.NoteThemeMergeHistory;
import com.newtech.note.entity.request.UpdateNoteThemeRequest;
import com.newtech.note.repositories.NoteThemeRepository;
import com.newtech.note.service.NoteEmbeddingService;
import com.newtech.note.service.NoteServiceV2;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NoteThemeServiceImplAtomicUpdateTest {
    @Test
    void renameUsesFieldUpdateAndPreservesHistoryAddedBetweenReads() {
        NoteThemeRepository repository = mock(NoteThemeRepository.class);
        ReactiveMongoTemplate mongoTemplate = mock(ReactiveMongoTemplate.class);
        NoteTheme theme = new NoteTheme();
        theme.setId("theme-1");
        theme.setDeviceId("owner");
        theme.setTheme("旧主题");
        theme.setDescription("原描述");
        NoteThemeMergeHistory concurrentHistory = new NoteThemeMergeHistory(
                "op-1", "merged-1", "合并标题", "摘要", List.of(), List.of(),
                LocalDateTime.of(2026, 9, 6, 12, 0));
        when(repository.findById("theme-1"))
                .thenReturn(Mono.just(theme), Mono.defer(() -> {
                    theme.setTheme("新主题");
                    theme.setMergeHistory(List.of(concurrentHistory));
                    return Mono.just(theme);
                }));
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(NoteTheme.class)))
                .thenReturn(Mono.just(UpdateResult.acknowledged(1, 1L, null)));
        NoteThemeServiceImpl service = new NoteThemeServiceImpl(
                repository, mock(NoteEmbeddingService.class), mock(NoteServiceV2.class),
                mock(EmbeddingClient.class), mongoTemplate);

        StepVerifier.create(service.updateNoteTheme("owner",
                        UpdateNoteThemeRequest.builder().id("theme-1").theme("新主题").build()))
                .assertNext(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getData().getMergeHistory()).containsExactly(concurrentHistory);
                })
                .verifyComplete();

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(any(Query.class), updateCaptor.capture(), eq(NoteTheme.class));
        assertThat(updateCaptor.getValue().getUpdateObject().toString())
                .contains("新主题")
                .doesNotContain("mergeHistory");
        verify(repository, never()).save(any(NoteTheme.class));
    }
}
