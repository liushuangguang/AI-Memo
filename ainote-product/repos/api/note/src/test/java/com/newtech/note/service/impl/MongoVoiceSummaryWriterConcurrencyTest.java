package com.newtech.note.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.newtech.note.client.DeepSeekClient;
import com.newtech.note.entity.dto.Note;
import com.newtech.note.entity.dto.noteModules.BacklogModule;
import com.newtech.note.entity.dto.noteModules.NoteModule;
import com.newtech.note.entity.dto.noteModules.TextDataModule;
import com.newtech.note.entity.request.voice.SaveVoiceDiscussionSummaryRequest;
import com.newtech.note.entity.vo.VoiceDiscussionSaveResult;
import com.newtech.note.security.NoteOwnershipService;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MongoVoiceSummaryWriterConcurrencyTest {
    private MongoServer server;
    private MongoClient client;
    private ReactiveMongoTemplate template;
    private VoiceDiscussionServiceImplV2 service;

    @BeforeEach
    void setUp() {
        server = new MongoServer(new MemoryBackend());
        server.bind("127.0.0.1", 0);
        client = MongoClients.create(server.getConnectionString());
        template = new ReactiveMongoTemplate(client, "voice-summary-concurrency");

        NoteOwnershipService ownership = mock(NoteOwnershipService.class);
        when(ownership.ownedNote(eq("note-1"), any())).thenAnswer(ignored ->
                template.findOne(ownedActiveNote(), Note.class));
        service = new VoiceDiscussionServiceImplV2(
                ownership,
                mock(DeepSeekClient.class),
                new ObjectMapper(),
                new MongoVoiceSummaryWriter(template));
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.shutdown();
    }

    @Test
    void concurrentSameKeyAndPayloadConvergesOnOneWriteAndTwoSuccesses() {
        insertNote();
        SaveVoiceDiscussionSummaryRequest request = request("同一总结", "same_key_123");

        StepVerifier.create(Mono.zip(save(request), save(request))
                        .flatMap(results -> template.findById("note-1", Note.class)
                                .map(note -> reactor.util.function.Tuples.of(results, note))))
                .assertNext(result -> {
                    assertThat(result.getT1().getT1().alreadySaved()
                            || result.getT1().getT2().alreadySaved()).isTrue();
                    assertThat(result.getT2().getVoiceSummarySaveDigests())
                            .containsOnlyKeys("same_key_123");
                    assertThat(text(result.getT2())).containsOnlyOnce("同一总结");
                    assertOtherModulePreserved(result.getT2());
                })
                .verifyComplete();
    }

    @Test
    void concurrentSameKeyDifferentPayloadAllowsOneSuccessAndOneConflict() {
        insertNote();

        StepVerifier.create(Mono.zip(
                        save(request("总结 A", "reused_key_123")).materialize(),
                        save(request("总结 B", "reused_key_123")).materialize()))
                .assertNext(results -> assertOneSuccessOneConflict(results.getT1(), results.getT2()))
                .verifyComplete();

        StepVerifier.create(template.findById("note-1", Note.class))
                .assertNext(note -> {
                    assertThat(note.getVoiceSummarySaveDigests()).hasSize(1);
                    String content = text(note);
                    boolean onlyA = content.contains("总结 A") && !content.contains("总结 B");
                    boolean onlyB = content.contains("总结 B") && !content.contains("总结 A");
                    assertThat(onlyA || onlyB).isTrue();
                    assertOtherModulePreserved(note);
                })
                .verifyComplete();
    }

    @Test
    void concurrentDifferentKeysNeverOverwriteTheWinningBody() {
        insertNote();

        StepVerifier.create(Mono.zip(
                        save(request("总结 A", "request_a_123")).materialize(),
                        save(request("总结 B", "request_b_123")).materialize()))
                .assertNext(results -> assertOneSuccessOneConflict(results.getT1(), results.getT2()))
                .verifyComplete();

        StepVerifier.create(template.findById("note-1", Note.class))
                .assertNext(note -> {
                    assertThat(note.getVoiceSummarySaveDigests()).hasSize(1);
                    assertOtherModulePreserved(note);
                })
                .verifyComplete();
    }

    private Mono<VoiceDiscussionSaveResult> save(SaveVoiceDiscussionSummaryRequest request) {
        return service.saveSummary(
                        request,
                        MockServerHttpRequest.post("/v2/voice-discussion/summary/save").build())
                .subscribeOn(Schedulers.parallel());
    }

    private void insertNote() {
        TextDataModule text = new TextDataModule();
        text.setModuleId("text-1");
        text.setContent("[{\"insert\":\"正文\\n\"}]");
        BacklogModule backlog = new BacklogModule();
        backlog.setModuleId("backlog-1");
        backlog.setName("不可改动的待办");
        Note note = new Note();
        note.setId("note-1");
        note.setDeviceId("owner-1");
        note.setModules(List.<NoteModule>of(backlog, text));
        template.insert(note).block();
    }

    private SaveVoiceDiscussionSummaryRequest request(String summary, String requestId) {
        return new SaveVoiceDiscussionSummaryRequest("note-1", summary, requestId);
    }

    private Query ownedActiveNote() {
        return Query.query(Criteria.where("_id").is("note-1")
                .and("deviceId").is("owner-1")
                .and("deleted").is(false));
    }

    private String text(Note note) {
        return ((TextDataModule) note.getModules().get(1)).getContent();
    }

    private void assertOtherModulePreserved(Note note) {
        BacklogModule backlog = (BacklogModule) note.getModules().getFirst();
        assertThat(backlog.getModuleId()).isEqualTo("backlog-1");
        assertThat(backlog.getName()).isEqualTo("不可改动的待办");
    }

    private void assertOneSuccessOneConflict(Signal<VoiceDiscussionSaveResult> first,
                                             Signal<VoiceDiscussionSaveResult> second) {
        assertThat(List.of(first, second).stream().filter(Signal::hasValue).count()).isEqualTo(1);
        List<Throwable> errors = List.of(first, second).stream()
                .map(Signal::getThrowable)
                .filter(java.util.Objects::nonNull)
                .toList();
        assertThat(errors).singleElement().satisfies(error -> {
            assertThat(error).isInstanceOf(ResponseStatusException.class);
            assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(409);
        });
    }
}
