package com.newtech.note.service.impl;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.newtech.note.entity.dto.NoteAnalysisRecord;
import com.newtech.note.entity.request.UpdateNoteAnalysisRecordRequest;
import com.newtech.note.repositories.NoteAnalysisRecordRepository;
import com.newtech.note.repositories.NoteRepository;
import com.newtech.note.repositories.impl.NoteAnalysisRecordDynamicRepositoryImpl;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NoteAnalysisMongoConcurrencyTest {
    private MongoServer server;
    private MongoClient client;
    private ReactiveMongoTemplate template;

    @BeforeEach
    void setUp() {
        server = new MongoServer(new MemoryBackend());
        server.bind("127.0.0.1", 0);
        client = MongoClients.create(server.getConnectionString());
        template = new ReactiveMongoTemplate(client, "analysis-concurrency");
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.shutdown();
    }

    @Test
    void repeatedConcurrentAtomicPatchesPreserveEverySuccessfulField() {
        NoteAnalysisRecord initial = new NoteAnalysisRecord();
        initial.setId("record");
        initial.setNoteId("note");
        template.insert(initial).block();
        NoteAnalysisRecordDynamicRepositoryImpl repository =
                new NoteAnalysisRecordDynamicRepositoryImpl(template);

        StepVerifier.create(Flux.range(0, 250)
                        .flatMap(index -> repository.patch("record", update(index)), 50)
                        .then(template.findById("record", NoteAnalysisRecord.class)))
                .assertNext(stored -> {
                    assertThat(stored.getOverallReview()).isNotBlank();
                    assertThat(stored.getAiSuggestion()).isNotBlank();
                    assertThat(stored.getAiIllustration()).isNotBlank();
                    assertThat(stored.getRelatedNoteIds()).isNotEmpty();
                    assertThat(stored.getRelatedLink()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void twoRepositoryInstancesAllocateUniqueMonotonicVersions() {
        NoteAnalysisRecordDynamicRepositoryImpl first =
                new NoteAnalysisRecordDynamicRepositoryImpl(template);
        NoteAnalysisRecordDynamicRepositoryImpl second =
                new NoteAnalysisRecordDynamicRepositoryImpl(template);

        StepVerifier.create(Flux.range(0, 100)
                        .flatMap(index -> (index & 1) == 0
                                ? first.allocateNextVersion("note", 0)
                                : second.allocateNextVersion("note", 0), 32)
                        .collectSortedList())
                .assertNext(versions -> assertThat(versions).containsExactlyElementsOf(
                        IntStream.rangeClosed(1, 100).boxed().toList()))
                .verifyComplete();
    }

    @Test
    void twoServiceInstancesConvergeOnOneSnapshotInSharedMongo() {
        NoteAnalysisRecordDynamicRepositoryImpl dynamic =
                new NoteAnalysisRecordDynamicRepositoryImpl(template);
        NoteAnalysisRecordServiceImpl first = new NoteAnalysisRecordServiceImpl(
                repositoryBackedBy(template, dynamic), mock(NoteRepository.class));
        NoteAnalysisRecordServiceImpl second = new NoteAnalysisRecordServiceImpl(
                repositoryBackedBy(template, dynamic), mock(NoteRepository.class));

        StepVerifier.create(Flux.range(0, 80)
                        .flatMap(index -> ((index & 1) == 0 ? first : second)
                                .findOrCreateForSnapshot("note", "same snapshot")
                                .subscribeOn(Schedulers.parallel()), 32)
                        .map(NoteAnalysisRecord::getId)
                        .collectList()
                        .flatMap(ids -> template.findAll(NoteAnalysisRecord.class).collectList()
                                .map(records -> reactor.util.function.Tuples.of(ids, records))))
                .assertNext(result -> {
                    assertThat(result.getT1()).hasSize(80).containsOnly(result.getT1().getFirst());
                    assertThat(result.getT2()).hasSize(1);
                    assertThat(result.getT2().getFirst().getRawNote()).isEqualTo("same snapshot");
                })
                .verifyComplete();
    }

    private UpdateNoteAnalysisRecordRequest update(int index) {
        UpdateNoteAnalysisRecordRequest request = new UpdateNoteAnalysisRecordRequest();
        request.setId("record");
        switch (index % 5) {
            case 0 -> request.setOverallReview("review-" + index);
            case 1 -> request.setAiSuggestion("suggestion-" + index);
            case 2 -> request.setAiIllustration("https://media.invalid/" + index);
            case 3 -> request.setRelatedNoteIds(List.of("related-" + index));
            default -> request.setRelatedLink(List.of());
        }
        return request;
    }

    private NoteAnalysisRecordRepository repositoryBackedBy(
            ReactiveMongoTemplate mongoTemplate,
            NoteAnalysisRecordDynamicRepositoryImpl dynamic) {
        NoteAnalysisRecordRepository repository = mock(NoteAnalysisRecordRepository.class);
        when(repository.findByNoteIdAndSnapshotHash(anyString(), anyString())).thenAnswer(invocation ->
                mongoTemplate.findOne(Query.query(Criteria.where("noteId")
                                .is(invocation.getArgument(0, String.class))
                                .and("snapshotHash").is(invocation.getArgument(1, String.class))),
                        NoteAnalysisRecord.class));
        when(repository.getLatestVersion(anyString())).thenAnswer(invocation ->
                dynamic.getLatestVersion(invocation.getArgument(0, String.class)));
        when(repository.findByNoteIdAndVersion(anyString(), org.mockito.ArgumentMatchers.anyInt())).thenAnswer(invocation ->
                mongoTemplate.findOne(Query.query(Criteria.where("noteId")
                                .is(invocation.getArgument(0, String.class))
                                .and("version").is(invocation.getArgument(1, Integer.class))),
                        NoteAnalysisRecord.class));
        when(repository.allocateNextVersion(anyString(), org.mockito.ArgumentMatchers.anyInt())).thenAnswer(invocation ->
                dynamic.allocateNextVersion(invocation.getArgument(0, String.class),
                        invocation.getArgument(1, Integer.class)));
        when(repository.insert(org.mockito.ArgumentMatchers.any(NoteAnalysisRecord.class))).thenAnswer(invocation ->
                mongoTemplate.insert(invocation.getArgument(0, NoteAnalysisRecord.class)));
        when(repository.findById(anyString())).thenAnswer(invocation ->
                mongoTemplate.findById(invocation.getArgument(0, String.class), NoteAnalysisRecord.class));
        return repository;
    }
}
