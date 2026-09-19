package com.newtech.note.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.UpdateResult;
import com.newtech.note.client.DeepSeekClient;
import com.newtech.note.client.DeepSeekCompletion;
import com.newtech.note.entity.dto.Note;
import com.newtech.note.entity.dto.NoteTheme;
import com.newtech.note.entity.dto.NoteThemeMergeHistory;
import com.newtech.note.entity.dto.NoteThemeMergeOperation;
import com.newtech.note.entity.dto.NoteThemeSourceRef;
import com.newtech.note.entity.dto.noteModules.TextDataModule;
import com.newtech.note.entity.request.MergeNoteThemeRequest;
import com.newtech.note.repositories.NoteRepository;
import com.newtech.note.repositories.NoteThemeMergeOperationRepository;
import com.newtech.note.repositories.NoteThemeRepository;
import com.newtech.note.service.NoteServiceV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.dao.DuplicateKeyException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class NoteThemeMergeServiceImplTest {
    private NoteThemeRepository themeRepository;
    private NoteThemeMergeOperationRepository operationRepository;
    private NoteRepository noteRepository;
    private NoteServiceV2 noteService;
    private DeepSeekClient deepSeekClient;
    private ReactiveMongoTemplate mongoTemplate;
    private NoteThemeMergeServiceImpl service;

    @BeforeEach
    void setUp() {
        themeRepository = mock(NoteThemeRepository.class);
        operationRepository = mock(NoteThemeMergeOperationRepository.class);
        noteRepository = mock(NoteRepository.class);
        noteService = mock(NoteServiceV2.class);
        deepSeekClient = mock(DeepSeekClient.class);
        mongoTemplate = mock(ReactiveMongoTemplate.class);
        service = new NoteThemeMergeServiceImpl(themeRepository, operationRepository,
                noteRepository, noteService, deepSeekClient, new ObjectMapper(), mongoTemplate);
    }

    @Test
    void candidatesDiscardProviderInventedIdsAndReturnOwnedSnapshots() {
        NoteTheme theme = theme("theme-1", "owner");
        Note owned = note("note-1", "owner", "可用标题", "可用内容");
        when(themeRepository.findById("theme-1")).thenReturn(Mono.just(theme));
        when(noteService.listRelatedNoteCandidates("owner", null)).thenReturn(Flux.just(owned));
        when(deepSeekClient.completeJsonWithUsage(anyString(), anyString())).thenReturn(Mono.just(
                new DeepSeekCompletion("""
                        {"candidates":[
                          {"id":"invented","score":1,"reason":"bad"},
                          {"id":"note-1","score":0.82,"reason":"同一主题"}
                        ]}
                        """, 42)));

        StepVerifier.create(service.candidates("owner", "theme-1"))
                .assertNext(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getData()).hasSize(1);
                    assertThat(response.getData().getFirst().getId()).isEqualTo("note-1");
                    assertThat(response.getData().getFirst().getReason()).isEqualTo("同一主题");
                })
                .verifyComplete();
    }

    @Test
    void candidatesScanPastSixtyNotesAndCanMatchAnOldNote() {
        NoteTheme theme = theme("theme-1", "owner");
        List<Note> notes = IntStream.range(0, 61)
                .mapToObj(index -> note("note-" + index, "owner",
                        "title-" + index, "content-" + index))
                .toList();
        when(themeRepository.findById("theme-1")).thenReturn(Mono.just(theme));
        when(noteService.listRelatedNoteCandidates("owner", null))
                .thenReturn(Flux.fromIterable(notes));
        when(deepSeekClient.completeJsonWithUsage(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String prompt = invocation.getArgument(1);
                    String response = prompt.contains("note-60")
                            ? "{\"candidates\":[{\"id\":\"note-60\",\"score\":0.91,\"reason\":\"旧笔记相关\"}]}"
                            : "{\"candidates\":[]}";
                    return Mono.just(new DeepSeekCompletion(response, 20));
                });

        StepVerifier.create(service.candidates("owner", "theme-1"))
                .assertNext(response -> assertThat(response.getData())
                        .extracting("id").containsExactly("note-60"))
                .verifyComplete();

        verify(deepSeekClient, times(3)).completeJsonWithUsage(anyString(), anyString());
    }

    @Test
    void mergeRequiresExplicitConfirmationBeforeAnyRead() {
        MergeNoteThemeRequest request = request(false);

        StepVerifier.create(service.merge("owner", "theme-1", request))
                .assertNext(response -> {
                    assertThat(response.isFailure()).isTrue();
                    assertThat(response.getMessage()).contains("确认");
                })
                .verifyComplete();

        verifyNoInteractions(themeRepository, noteRepository, deepSeekClient);
    }

    @Test
    void retryReusesGeneratedNoteAndNeverSavesSources() {
        NoteTheme theme = theme("theme-1", "owner");
        Note sourceA = note("note-a", "owner", "A", "内容A");
        sourceA.setImageUrl("https://example.test/a.jpg");
        Note sourceB = note("note-b", "owner", "B", "内容B");
        MergeNoteThemeRequest request = request(true);
        AtomicReference<NoteThemeMergeOperation> operation = new AtomicReference<>();
        AtomicReference<Note> mergedNote = new AtomicReference<>();
        AtomicInteger operationReads = new AtomicInteger();

        when(themeRepository.findById("theme-1")).thenReturn(Mono.just(theme));
        when(operationRepository.findById(anyString())).thenAnswer(invocation ->
                operationReads.getAndIncrement() == 0
                        ? Mono.empty()
                        : Mono.just(operation.get()));
        when(operationRepository.insert(any(NoteThemeMergeOperation.class))).thenAnswer(invocation -> {
            NoteThemeMergeOperation saved = invocation.getArgument(0);
            operation.set(saved);
            return Mono.just(saved);
        });
        when(operationRepository.save(any())).thenAnswer(invocation -> {
            NoteThemeMergeOperation saved = invocation.getArgument(0);
            operation.set(saved);
            return Mono.just(saved);
        });
        when(noteRepository.findAllById(any(Iterable.class)))
                .thenReturn(Flux.just(sourceA, sourceB));
        when(deepSeekClient.completeJsonWithUsage(anyString(), anyString())).thenReturn(Mono.just(
                new DeepSeekCompletion(
                        "{\"title\":\"合并标题\",\"content\":\"合并正文\"}", 30)));
        when(noteRepository.findById(anyString())).thenAnswer(invocation ->
                mergedNote.get() == null ? Mono.empty() : Mono.just(mergedNote.get()));
        when(noteRepository.insert(any(Note.class))).thenAnswer(invocation -> {
            Note saved = invocation.getArgument(0);
            mergedNote.set(saved);
            return Mono.just(saved);
        });
        when(mongoTemplate.updateFirst(any(), any(), eq(NoteTheme.class)))
                .thenReturn(Mono.just(UpdateResult.acknowledged(1, 1L, null)));
        when(mongoTemplate.updateFirst(any(), any(), eq(Note.class)))
                .thenReturn(Mono.just(UpdateResult.acknowledged(1, 1L, null)));

        StepVerifier.create(service.merge("owner", "theme-1", request))
                .assertNext(response -> assertThat(response.getData().getNote().getTitle())
                        .isEqualTo("合并标题"))
                .verifyComplete();
        StepVerifier.create(service.merge("owner", "theme-1", request))
                .assertNext(response -> assertThat(response.getData().getHistory().getSources())
                        .extracting("noteId").containsExactly("note-a", "note-b"))
                .verifyComplete();

        ArgumentCaptor<Note> insertedCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository, times(1)).insert(insertedCaptor.capture());
        assertThat(insertedCaptor.getValue().getId()).startsWith("theme-merge-note-");
        assertThat(insertedCaptor.getValue().getDeviceId()).isEqualTo("owner");
        assertThat(insertedCaptor.getValue().getMergeOperationId()).isNotBlank();
        assertThat(insertedCaptor.getValue().getImageUrl()).isEqualTo("https://example.test/a.jpg");
        assertThat(insertedCaptor.getValue().getModules()).isNotEmpty();
        ArgumentCaptor<Query> activationQuery = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate, times(1)).updateFirst(
                activationQuery.capture(), any(), eq(Note.class));
        assertThat(activationQuery.getValue().getQueryObject())
                .containsEntry("mergeOperationId",
                        insertedCaptor.getValue().getMergeOperationId());
        verify(deepSeekClient, times(1)).completeJsonWithUsage(anyString(), anyString());
        verify(noteRepository, never()).insert(sourceA);
        verify(noteRepository, never()).insert(sourceB);
    }

    @Test
    void providerCompletionAfterThemeDeletionCreatesNoNote() {
        NoteTheme theme = theme("theme-1", "owner");
        Note sourceA = note("note-a", "owner", "A", "内容A");
        Note sourceB = note("note-b", "owner", "B", "内容B");
        MergeNoteThemeRequest request = request(true);
        when(themeRepository.findById("theme-1"))
                .thenReturn(Mono.just(theme), Mono.empty());
        when(operationRepository.findById(anyString())).thenReturn(Mono.empty());
        when(operationRepository.insert(any(NoteThemeMergeOperation.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(operationRepository.save(any(NoteThemeMergeOperation.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(noteRepository.findAllById(any(Iterable.class)))
                .thenReturn(Flux.just(sourceA, sourceB));
        when(deepSeekClient.completeJsonWithUsage(anyString(), anyString()))
                .thenReturn(Mono.just(new DeepSeekCompletion(
                        "{\"title\":\"合并标题\",\"content\":\"合并正文\"}", 30)));

        StepVerifier.create(service.merge("owner", "theme-1", request))
                .assertNext(response -> assertThat(response.isFailure()).isTrue())
                .verifyComplete();

        verify(noteRepository, never()).insert(any(Note.class));
        verify(mongoTemplate, never()).updateFirst(any(), any(), eq(NoteTheme.class));
    }

    @Test
    void providerCompletionAfterSourceDeletionCreatesNoNote() {
        NoteTheme theme = theme("theme-1", "owner");
        Note sourceA = note("note-a", "owner", "A", "内容A");
        Note sourceB = note("note-b", "owner", "B", "内容B");
        when(themeRepository.findById("theme-1")).thenReturn(Mono.just(theme));
        when(operationRepository.findById(anyString())).thenReturn(Mono.empty());
        when(operationRepository.insert(any(NoteThemeMergeOperation.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(operationRepository.save(any(NoteThemeMergeOperation.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(noteRepository.findAllById(any(Iterable.class)))
                .thenReturn(Flux.just(sourceA, sourceB), Flux.just(sourceA));
        when(deepSeekClient.completeJsonWithUsage(anyString(), anyString()))
                .thenReturn(Mono.just(new DeepSeekCompletion(
                        "{\"title\":\"合并标题\",\"content\":\"合并正文\"}", 30)));

        StepVerifier.create(service.merge("owner", "theme-1", request(true)))
                .assertNext(response -> assertThat(response.isFailure()).isTrue())
                .verifyComplete();

        verify(noteRepository, never()).insert(any(Note.class));
        verify(mongoTemplate, never()).updateFirst(any(), any(), eq(NoteTheme.class));
    }

    @Test
    void concurrentSameKeyUsesOneProviderOneNoteAndOneHistoryWrite() throws Exception {
        NoteTheme theme = theme("theme-1", "owner");
        Note sourceA = note("note-a", "owner", "A", "内容A");
        Note sourceB = note("note-b", "owner", "B", "内容B");
        MergeNoteThemeRequest request = request(true);
        AtomicReference<NoteThemeMergeOperation> operation = new AtomicReference<>();
        AtomicReference<Note> mergedNote = new AtomicReference<>();
        AtomicInteger findReads = new AtomicInteger();
        AtomicInteger insertCalls = new AtomicInteger();
        Sinks.One<NoteThemeMergeOperation> firstInsert = Sinks.one();

        when(themeRepository.findById("theme-1")).thenReturn(Mono.just(theme));
        when(operationRepository.findById(anyString())).thenAnswer(invocation -> {
            int read = findReads.incrementAndGet();
            return read <= 2 ? Mono.empty() : Mono.just(operation.get());
        });
        when(operationRepository.insert(any(NoteThemeMergeOperation.class)))
                .thenAnswer(invocation -> {
                    NoteThemeMergeOperation candidate = invocation.getArgument(0);
                    operation.compareAndSet(null, candidate);
                    if (insertCalls.incrementAndGet() == 1) return firstInsert.asMono();
                    return Mono.error(new DuplicateKeyException("same operation"));
                });
        when(operationRepository.save(any(NoteThemeMergeOperation.class)))
                .thenAnswer(invocation -> {
                    NoteThemeMergeOperation saved = invocation.getArgument(0);
                    operation.set(saved);
                    return Mono.just(saved);
                });
        when(noteRepository.findAllById(any(Iterable.class)))
                .thenReturn(Flux.just(sourceA, sourceB));
        when(deepSeekClient.completeJsonWithUsage(anyString(), anyString()))
                .thenReturn(Mono.just(new DeepSeekCompletion(
                        "{\"title\":\"合并标题\",\"content\":\"合并正文\"}", 30)));
        when(noteRepository.findById(anyString())).thenAnswer(invocation ->
                mergedNote.get() == null ? Mono.empty() : Mono.just(mergedNote.get()));
        when(noteRepository.insert(any(Note.class))).thenAnswer(invocation -> {
            Note inserted = invocation.getArgument(0);
            mergedNote.set(inserted);
            return Mono.just(inserted);
        });
        when(mongoTemplate.updateFirst(any(), any(), eq(NoteTheme.class)))
                .thenReturn(Mono.just(UpdateResult.acknowledged(1, 1L, null)));
        when(mongoTemplate.updateFirst(any(), any(), eq(Note.class)))
                .thenReturn(Mono.just(UpdateResult.acknowledged(1, 1L, null)));

        var firstResult = service.merge("owner", "theme-1", request).toFuture();
        var duplicateResult = service.merge("owner", "theme-1", request).block();
        assertThat(duplicateResult).isNotNull();
        assertThat(duplicateResult.getMessage()).contains("进行中");

        firstInsert.tryEmitValue(operation.get());
        assertThat(firstResult.get(2, TimeUnit.SECONDS).isSuccess()).isTrue();

        verify(deepSeekClient, times(1)).completeJsonWithUsage(anyString(), anyString());
        verify(noteRepository, times(1)).insert(any(Note.class));
        verify(mongoTemplate, times(1)).updateFirst(any(), any(), eq(NoteTheme.class));
    }

    @Test
    void markCompleteFailureKeepsGeneratedOperationAndRetryDoesNotRegenerate() {
        NoteTheme theme = theme("theme-1", "owner");
        Note sourceA = note("note-a", "owner", "A", "内容A");
        Note sourceB = note("note-b", "owner", "B", "内容B");
        MergeNoteThemeRequest request = request(true);
        AtomicReference<NoteThemeMergeOperation> operation = new AtomicReference<>();
        AtomicReference<Note> mergedNote = new AtomicReference<>();
        AtomicInteger completeSaves = new AtomicInteger();

        when(themeRepository.findById("theme-1")).thenReturn(Mono.just(theme));
        when(operationRepository.findById(anyString())).thenAnswer(invocation ->
                operation.get() == null ? Mono.empty() : Mono.just(operation.get()));
        when(operationRepository.insert(any(NoteThemeMergeOperation.class))).thenAnswer(invocation -> {
            NoteThemeMergeOperation inserted = invocation.getArgument(0);
            operation.set(inserted);
            return Mono.just(inserted);
        });
        when(operationRepository.save(any(NoteThemeMergeOperation.class))).thenAnswer(invocation -> {
            NoteThemeMergeOperation saved = invocation.getArgument(0);
            if ("COMPLETE".equals(saved.getStatus()) && completeSaves.getAndIncrement() == 0) {
                return Mono.error(new IllegalStateException("complete save failed"));
            }
            operation.set(saved);
            return Mono.just(saved);
        });
        when(noteRepository.findAllById(any(Iterable.class)))
                .thenReturn(Flux.just(sourceA, sourceB));
        when(deepSeekClient.completeJsonWithUsage(anyString(), anyString())).thenReturn(Mono.just(
                new DeepSeekCompletion(
                        "{\"title\":\"合并标题\",\"content\":\"合并正文\"}", 30)));
        when(noteRepository.findById(anyString())).thenAnswer(invocation ->
                mergedNote.get() == null ? Mono.empty() : Mono.just(mergedNote.get()));
        when(noteRepository.insert(any(Note.class))).thenAnswer(invocation -> {
            Note inserted = invocation.getArgument(0);
            mergedNote.set(inserted);
            return Mono.just(inserted);
        });
        when(mongoTemplate.updateFirst(any(), any(), eq(NoteTheme.class)))
                .thenReturn(Mono.just(UpdateResult.acknowledged(1, 1L, null)));
        when(mongoTemplate.updateFirst(any(), any(), eq(Note.class)))
                .thenReturn(Mono.just(UpdateResult.acknowledged(1, 1L, null)));

        StepVerifier.create(service.merge("owner", "theme-1", request))
                .assertNext(response -> assertThat(response.isFailure()).isTrue())
                .verifyComplete();

        assertThat(operation.get().getStatus()).isEqualTo("GENERATED");
        theme.setMergeHistory(List.of(new NoteThemeMergeHistory(
                operation.get().getId(), operation.get().getMergedNoteId(),
                operation.get().getMergedTitle(), "合并正文",
                operation.get().getImageUrls(), operation.get().getSources(),
                operation.get().getMergedAt())));

        StepVerifier.create(service.merge("owner", "theme-1", request))
                .assertNext(response -> assertThat(response.isSuccess()).isTrue())
                .verifyComplete();

        verify(deepSeekClient, times(1)).completeJsonWithUsage(anyString(), anyString());
        verify(noteRepository, times(1)).insert(any(Note.class));
        verify(mongoTemplate, times(1)).updateFirst(any(), any(), eq(NoteTheme.class));
        assertThat(theme.getMergeHistory()).hasSize(1);
        assertThat(operation.get().getStatus()).isEqualTo("COMPLETE");
    }

    @Test
    void mergePromptPreservesFactsAtEndOfSourceContent() {
        NoteTheme theme = theme("theme-1", "owner");
        String tailFact = "尾部事实-航班改到周五晚八点";
        Note sourceA = note("note-a", "owner", "A", "前文".repeat(2_500) + tailFact);
        Note sourceB = note("note-b", "owner", "B", "内容B");
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        stubSuccessfulNewMerge(theme, sourceA, sourceB);
        when(deepSeekClient.completeJsonWithUsage(anyString(), promptCaptor.capture()))
                .thenReturn(Mono.just(new DeepSeekCompletion(
                        "{\"title\":\"合并标题\",\"content\":\"合并正文\"}", 30)));

        StepVerifier.create(service.merge("owner", "theme-1", request(true)))
                .assertNext(response -> assertThat(response.isSuccess()).isTrue())
                .verifyComplete();

        assertThat(promptCaptor.getValue()).contains(tailFact);
    }

    @Test
    void mergeRejectsSourcesOverTwentyFourThousandCharactersWithoutCallingProvider() {
        NoteTheme theme = theme("theme-1", "owner");
        Note sourceA = note("note-a", "owner", "A", "甲".repeat(12_100));
        Note sourceB = note("note-b", "owner", "B", "乙".repeat(12_100));

        when(themeRepository.findById("theme-1")).thenReturn(Mono.just(theme));
        when(operationRepository.findById(anyString())).thenReturn(Mono.empty());
        when(operationRepository.insert(any(NoteThemeMergeOperation.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(operationRepository.save(any(NoteThemeMergeOperation.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(noteRepository.findAllById(any(Iterable.class)))
                .thenReturn(Flux.just(sourceA, sourceB));

        StepVerifier.create(service.merge("owner", "theme-1", request(true)))
                .assertNext(response -> {
                    assertThat(response.isFailure()).isTrue();
                    assertThat(response.getMessage()).isEqualTo(
                            "所选备忘录总内容超过24000字符，请减少来源后重试");
                })
                .verifyComplete();

        verifyNoInteractions(deepSeekClient);
        verify(noteRepository, never()).insert(any(Note.class));
    }

    @Test
    void mergeRejectsProviderTitleOverOneHundredTwentyCharactersWithoutTruncating() {
        NoteTheme theme = theme("theme-1", "owner");
        stubSuccessfulNewMerge(theme,
                note("note-a", "owner", "A", "内容A"),
                note("note-b", "owner", "B", "内容B"));
        String oversizedTitle = "题".repeat(121);
        when(deepSeekClient.completeJsonWithUsage(anyString(), anyString()))
                .thenReturn(Mono.just(new DeepSeekCompletion(
                        "{\"title\":\"" + oversizedTitle
                                + "\",\"content\":\"合并正文\"}", 30)));

        StepVerifier.create(service.merge("owner", "theme-1", request(true)))
                .assertNext(response -> {
                    assertThat(response.isFailure()).isTrue();
                    assertThat(response.getMessage()).isEqualTo("合并标题超过120字符");
                })
                .verifyComplete();

        verify(noteRepository, never()).insert(any(Note.class));
    }

    @Test
    void mergeRejectsProviderContentOverTwentyThousandCharactersWithoutTruncating() {
        NoteTheme theme = theme("theme-1", "owner");
        stubSuccessfulNewMerge(theme,
                note("note-a", "owner", "A", "内容A"),
                note("note-b", "owner", "B", "内容B"));
        String oversizedContent = "文".repeat(20_001);
        when(deepSeekClient.completeJsonWithUsage(anyString(), anyString()))
                .thenReturn(Mono.just(new DeepSeekCompletion(
                        "{\"title\":\"合并标题\",\"content\":\""
                                + oversizedContent + "\"}", 30)));

        StepVerifier.create(service.merge("owner", "theme-1", request(true)))
                .assertNext(response -> {
                    assertThat(response.isFailure()).isTrue();
                    assertThat(response.getMessage()).isEqualTo("合并内容超过20000字符");
                })
                .verifyComplete();

        verify(noteRepository, never()).insert(any(Note.class));
    }

    @Test
    void generatedRetryRejectsSameOwnerNoteBoundToDifferentOperation() {
        NoteTheme theme = theme("theme-1", "owner");
        NoteThemeMergeOperation operation = generatedOperation("GENERATED");
        Note collision = note(operation.getMergedNoteId(), "owner", "其他笔记", "其他内容");
        collision.setMergeOperationId("different-operation");

        when(themeRepository.findById("theme-1")).thenReturn(Mono.just(theme));
        when(operationRepository.findById(anyString())).thenReturn(Mono.just(operation));
        when(operationRepository.insert(any(NoteThemeMergeOperation.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(noteRepository.findAllById(any(Iterable.class))).thenReturn(Flux.just(
                note("note-a", "owner", "A", "内容A"),
                note("note-b", "owner", "B", "内容B")));
        when(noteRepository.findById(operation.getMergedNoteId())).thenReturn(Mono.just(collision));

        StepVerifier.create(service.merge("owner", "theme-1", request(true)))
                .assertNext(response -> assertThat(response.isFailure()).isTrue())
                .verifyComplete();

        verifyNoInteractions(deepSeekClient);
        verify(noteRepository, never()).insert(any(Note.class));
        verify(mongoTemplate, never()).updateFirst(any(), any(), eq(NoteTheme.class));
    }

    @Test
    void completeRetryRejectsSameOwnerNoteBoundToDifferentOperation() {
        NoteTheme theme = theme("theme-1", "owner");
        NoteThemeMergeOperation operation = generatedOperation("COMPLETE");
        Note collision = note(operation.getMergedNoteId(), "owner", "其他笔记", "其他内容");
        collision.setMergeOperationId("different-operation");

        when(themeRepository.findById("theme-1")).thenReturn(Mono.just(theme));
        when(operationRepository.findById(anyString())).thenReturn(Mono.just(operation));
        when(operationRepository.insert(any(NoteThemeMergeOperation.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(noteRepository.findById(operation.getMergedNoteId())).thenReturn(Mono.just(collision));

        StepVerifier.create(service.merge("owner", "theme-1", request(true)))
                .assertNext(response -> assertThat(response.isFailure()).isTrue())
                .verifyComplete();

        verifyNoInteractions(deepSeekClient);
        verify(mongoTemplate, never()).updateFirst(any(), any(), eq(NoteTheme.class));
    }

    @Test
    void activationFallbackRejectsSameOwnerNoteBoundToDifferentOperation() {
        NoteTheme theme = theme("theme-1", "owner");
        NoteThemeMergeOperation operation = generatedOperation("GENERATED");
        Note boundDraft = note(operation.getMergedNoteId(), "owner", "合并标题", "合并正文");
        boundDraft.setMergeOperationId(operation.getId());
        boundDraft.setDeleted(true);
        Note collision = note(operation.getMergedNoteId(), "owner", "其他笔记", "其他内容");
        collision.setMergeOperationId("different-operation");

        when(themeRepository.findById("theme-1")).thenReturn(Mono.just(theme));
        when(operationRepository.findById(anyString())).thenReturn(Mono.just(operation));
        when(operationRepository.insert(any(NoteThemeMergeOperation.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(noteRepository.findAllById(any(Iterable.class))).thenReturn(Flux.just(
                note("note-a", "owner", "A", "内容A"),
                note("note-b", "owner", "B", "内容B")));
        when(noteRepository.findById(operation.getMergedNoteId()))
                .thenReturn(Mono.just(boundDraft), Mono.just(collision));
        when(mongoTemplate.updateFirst(any(), any(), eq(NoteTheme.class)))
                .thenReturn(Mono.just(UpdateResult.acknowledged(1, 1L, null)));
        when(mongoTemplate.updateFirst(any(), any(), eq(Note.class)))
                .thenReturn(Mono.just(UpdateResult.acknowledged(0, 0L, null)));

        StepVerifier.create(service.merge("owner", "theme-1", request(true)))
                .assertNext(response -> assertThat(response.isFailure()).isTrue())
                .verifyComplete();

        verifyNoInteractions(deepSeekClient);
        verify(operationRepository, never()).save(any(NoteThemeMergeOperation.class));
        verify(mongoTemplate, times(1)).updateFirst(any(), any(), eq(Note.class));
    }

    @Test
    void concurrentStaleProcessingRetriesUseCasSoOnlyLeaseWinnerRegenerates() throws Exception {
        NoteTheme theme = theme("theme-1", "owner");
        Note sourceA = note("note-a", "owner", "A", "内容A");
        Note sourceB = note("note-b", "owner", "B", "内容B");
        LocalDateTime staleAt = LocalDateTime.now().minusMinutes(4);
        NoteThemeMergeOperation firstSnapshot = operation("PROCESSING", staleAt);
        NoteThemeMergeOperation secondSnapshot = operation("PROCESSING", staleAt);
        AtomicReference<NoteThemeMergeOperation> persisted = new AtomicReference<>();
        AtomicReference<Note> mergedNote = new AtomicReference<>();
        AtomicInteger operationReads = new AtomicInteger();
        AtomicInteger leaseAttempts = new AtomicInteger();
        Sinks.One<DeepSeekCompletion> providerCompletion = Sinks.one();

        when(themeRepository.findById("theme-1")).thenReturn(Mono.just(theme));
        when(operationRepository.findById(anyString())).thenAnswer(invocation -> {
            int read = operationReads.incrementAndGet();
            if (read == 1) return Mono.just(firstSnapshot);
            if (read == 2) return Mono.just(secondSnapshot);
            return Mono.just(persisted.get());
        });
        when(operationRepository.insert(any(NoteThemeMergeOperation.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(mongoTemplate.updateFirst(any(), any(), eq(NoteThemeMergeOperation.class)))
                .thenAnswer(invocation -> {
                    if (leaseAttempts.incrementAndGet() == 1) {
                        persisted.set(operation("PROCESSING", LocalDateTime.now()));
                        return Mono.just(UpdateResult.acknowledged(1, 1L, null));
                    }
                    return Mono.just(UpdateResult.acknowledged(0, 0L, null));
                });
        when(operationRepository.save(any(NoteThemeMergeOperation.class))).thenAnswer(invocation -> {
            NoteThemeMergeOperation saved = invocation.getArgument(0);
            persisted.set(saved);
            return Mono.just(saved);
        });
        when(noteRepository.findAllById(any(Iterable.class)))
                .thenReturn(Flux.just(sourceA, sourceB));
        when(deepSeekClient.completeJsonWithUsage(anyString(), anyString()))
                .thenReturn(providerCompletion.asMono());
        when(noteRepository.findById(anyString())).thenAnswer(invocation ->
                mergedNote.get() == null ? Mono.empty() : Mono.just(mergedNote.get()));
        when(noteRepository.insert(any(Note.class))).thenAnswer(invocation -> {
            Note inserted = invocation.getArgument(0);
            mergedNote.set(inserted);
            return Mono.just(inserted);
        });
        when(mongoTemplate.updateFirst(any(), any(), eq(NoteTheme.class)))
                .thenReturn(Mono.just(UpdateResult.acknowledged(1, 1L, null)));
        when(mongoTemplate.updateFirst(any(), any(), eq(Note.class)))
                .thenReturn(Mono.just(UpdateResult.acknowledged(1, 1L, null)));

        var winner = service.merge("owner", "theme-1", request(true)).toFuture();
        var loser = service.merge("owner", "theme-1", request(true)).block();
        assertThat(loser).isNotNull();
        assertThat(loser.isFailure()).isTrue();
        assertThat(loser.getMessage()).contains("仍在进行中");

        providerCompletion.tryEmitValue(new DeepSeekCompletion(
                "{\"title\":\"合并标题\",\"content\":\"合并正文\"}", 30));
        assertThat(winner.get(2, TimeUnit.SECONDS).isSuccess()).isTrue();

        ArgumentCaptor<Query> leaseQuery = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate, times(2)).updateFirst(leaseQuery.capture(), any(),
                eq(NoteThemeMergeOperation.class));
        assertThat(leaseQuery.getAllValues()).allSatisfy(query ->
                assertThat(query.getQueryObject())
                        .containsEntry("_id", firstSnapshot.getId())
                        .containsEntry("deviceId", "owner")
                        .containsEntry("themeId", "theme-1")
                        .containsEntry("status", "PROCESSING")
                        .containsEntry("updatedAt", staleAt));
        verify(deepSeekClient, times(1)).completeJsonWithUsage(anyString(), anyString());
        verify(noteRepository, times(1)).insert(any(Note.class));
        verify(mongoTemplate, times(1)).updateFirst(any(), any(), eq(NoteTheme.class));
    }

    private void stubSuccessfulNewMerge(NoteTheme theme, Note sourceA, Note sourceB) {
        when(themeRepository.findById("theme-1")).thenReturn(Mono.just(theme));
        when(operationRepository.findById(anyString())).thenReturn(Mono.empty());
        when(operationRepository.insert(any(NoteThemeMergeOperation.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(operationRepository.save(any(NoteThemeMergeOperation.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(noteRepository.findAllById(any(Iterable.class)))
                .thenReturn(Flux.just(sourceA, sourceB));
        when(noteRepository.findById(anyString())).thenReturn(Mono.empty());
        when(noteRepository.insert(any(Note.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(mongoTemplate.updateFirst(any(), any(), eq(NoteTheme.class)))
                .thenReturn(Mono.just(UpdateResult.acknowledged(1, 1L, null)));
        when(mongoTemplate.updateFirst(any(), any(), eq(Note.class)))
                .thenReturn(Mono.just(UpdateResult.acknowledged(1, 1L, null)));
    }

    private static NoteThemeMergeOperation operation(String status, LocalDateTime updatedAt) {
        NoteThemeMergeOperation operation = new NoteThemeMergeOperation();
        operation.setId("operation-1");
        operation.setDeviceId("owner");
        operation.setThemeId("theme-1");
        operation.setRequestKey("request-12345678");
        operation.setStatus(status);
        operation.setMergedNoteId("theme-merge-note-operation-1");
        operation.setSources(List.of(
                new NoteThemeSourceRef("note-a", ""),
                new NoteThemeSourceRef("note-b", "")));
        operation.setUpdatedAt(updatedAt);
        return operation;
    }

    private static NoteThemeMergeOperation generatedOperation(String status) {
        NoteThemeMergeOperation operation = operation(status, LocalDateTime.now());
        operation.setMergedTitle("合并标题");
        operation.setMergedContent("合并正文");
        operation.setImageUrls(List.of());
        operation.setMergedAt(LocalDateTime.now());
        return operation;
    }

    private static NoteTheme theme(String id, String owner) {
        NoteTheme theme = new NoteTheme();
        theme.setId(id);
        theme.setDeviceId(owner);
        theme.setTheme("旅行计划");
        theme.setDescription("整合行程与注意事项");
        return theme;
    }

    private static Note note(String id, String owner, String title, String content) {
        TextDataModule text = new TextDataModule();
        text.setContent("[{\"insert\":\"" + content + "\\n\"}]");
        Note note = new Note();
        note.setId(id);
        note.setDeviceId(owner);
        note.setTitle(title);
        note.setDimension(0);
        note.setModules(List.of(text));
        return note;
    }

    private static MergeNoteThemeRequest request(boolean confirmed) {
        MergeNoteThemeRequest request = new MergeNoteThemeRequest();
        request.setSelectionConfirmed(confirmed);
        request.setIdempotencyKey("request-12345678");
        request.setSourceNoteIds(List.of("note-a", "note-b"));
        return request;
    }
}
