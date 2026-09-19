package com.newtech.note.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtech.note.client.DeepSeekClient;
import com.newtech.note.client.DeepSeekCompletion;
import com.newtech.note.common.BusinessException;
import com.newtech.note.entity.dto.Note;
import com.newtech.note.entity.dto.noteModules.NoteModule;
import com.newtech.note.entity.dto.noteModules.TextDataModule;
import com.newtech.note.entity.request.voice.SaveVoiceDiscussionSummaryRequest;
import com.newtech.note.entity.request.voice.VoiceDiscussionRequest;
import com.newtech.note.entity.request.voice.VoiceDiscussionSummaryRequest;
import com.newtech.note.entity.request.voice.VoiceDiscussionTurn;
import com.newtech.note.security.NoteOwnershipService;
import com.newtech.note.service.VoiceSummaryWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VoiceDiscussionServiceImplV2Test {
    private NoteOwnershipService ownershipService;
    private DeepSeekClient deepSeekClient;
    private VoiceSummaryWriter summaryWriter;
    private VoiceDiscussionServiceImplV2 service;
    private ServerHttpRequest httpRequest;

    @BeforeEach
    void setUp() {
        ownershipService = mock(NoteOwnershipService.class);
        deepSeekClient = mock(DeepSeekClient.class);
        summaryWriter = mock(VoiceSummaryWriter.class);
        service = new VoiceDiscussionServiceImplV2(
                ownershipService, deepSeekClient, new ObjectMapper(), summaryWriter);
        httpRequest = MockServerHttpRequest.post("/v2/voice-discussion/respond").build();
    }

    @Test
    void respondUsesOwnedBoundedNoteContextAndRecentHistory() {
        Note note = note("note-1", "owner-1", "x".repeat(7_000));
        when(ownershipService.ownedNote("note-1", httpRequest)).thenReturn(Mono.just(note));
        when(deepSeekClient.completeTextWithUsage(any(), any()))
                .thenReturn(Mono.just(new DeepSeekCompletion("聚焦下一步。", 20)));
        List<VoiceDiscussionTurn> history = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            history.add(new VoiceDiscussionTurn(i % 2 == 0 ? "user" : "assistant",
                    "turn-" + i));
        }
        history.add(new VoiceDiscussionTurn("system", "must-not-be-forwarded"));

        StepVerifier.create(service.respond(
                        new VoiceDiscussionRequest("note-1", "我们先做什么？", history), httpRequest))
                .assertNext(reply -> assertEquals("聚焦下一步。", reply.reply()))
                .verifyComplete();

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(deepSeekClient).completeTextWithUsage(any(), prompt.capture());
        assertFalse(prompt.getValue().contains("turn-0"));
        assertTrue(prompt.getValue().contains("turn-14"));
        assertFalse(prompt.getValue().contains("must-not-be-forwarded"));
        assertFalse(prompt.getValue().contains("x".repeat(6_001)));
        assertTrue(prompt.getValue().contains("我们先做什么？"));
    }

    @Test
    void ownershipFailureStopsBeforeDeepSeek() {
        when(ownershipService.ownedNote("note-1", httpRequest))
                .thenReturn(Mono.error(new BusinessException("NOTE_FORBIDDEN", "forbidden")));

        StepVerifier.create(service.respond(
                        new VoiceDiscussionRequest("note-1", "hello", List.of()), httpRequest))
                .expectErrorMatches(error -> error instanceof BusinessException
                        && "NOTE_FORBIDDEN".equals(((BusinessException) error).getCode()))
                .verify();

        verify(deepSeekClient, never()).completeTextWithUsage(any(), any());
    }

    @Test
    void summaryRequiresConversationBeforeProviderCall() {
        StepVerifier.create(service.previewSummary(
                        new VoiceDiscussionSummaryRequest("note-1", List.of()), httpRequest))
                .expectErrorMatches(error -> error.getMessage().contains("400"))
                .verify();

        verify(ownershipService, never()).ownedNote(any(), any());
        verify(deepSeekClient, never()).completeTextWithUsage(any(), any());
    }

    @Test
    void confirmedSummaryUsesAtomicWriterAndRetryIsIdempotent() {
        Note note = note("note-1", "owner-1", "[{\"insert\":\"正文\\n\"}]");
        TextDataModule textModule = (TextDataModule) note.getModules().getFirst();
        when(ownershipService.ownedNote("note-1", httpRequest)).thenReturn(Mono.just(note));
        when(summaryWriter.compareAndSetTextContent(
                eq("note-1"), eq("owner-1"), eq("text-1"), eq(0), any(), any(),
                eq("save_request_123"), any()))
                .thenAnswer(invocation -> {
                    textModule.setContent(invocation.getArgument(5));
                    note.setVoiceSummarySaveDigests(Map.of(
                            invocation.getArgument(6), invocation.getArgument(7)));
                    return Mono.just(note);
                });
        SaveVoiceDiscussionSummaryRequest request = new SaveVoiceDiscussionSummaryRequest(
                "note-1", "先验证需求，再实现。", "save_request_123");

        StepVerifier.create(service.saveSummary(request, httpRequest))
                .assertNext(result -> {
                    assertFalse(result.alreadySaved());
                    assertEquals(note, result.note());
                })
                .verifyComplete();
        StepVerifier.create(service.saveSummary(request, httpRequest))
                .assertNext(result -> assertTrue(result.alreadySaved()))
                .verifyComplete();

        ArgumentCaptor<String> newContent = ArgumentCaptor.forClass(String.class);
        verify(summaryWriter).compareAndSetTextContent(
                eq("note-1"),
                eq("owner-1"),
                eq("text-1"),
                eq(0),
                eq("[{\"insert\":\"正文\\n\"}]"),
                newContent.capture(),
                eq("save_request_123"),
                any());
        assertTrue(newContent.getValue().contains("语音讨论总结"));
        assertTrue(newContent.getValue().contains("先验证需求，再实现。"));
    }

    @Test
    void casConflictReturns409WithoutOverwritingNewerBody() {
        Note snapshot = note("note-1", "owner-1", "[{\"insert\":\"旧正文\\n\"}]");
        Note latest = note("note-1", "owner-1", "[{\"insert\":\"另一端的新正文\\n\"}]");
        when(ownershipService.ownedNote("note-1", httpRequest)).thenReturn(Mono.just(snapshot));
        when(summaryWriter.compareAndSetTextContent(
                any(), any(), any(), anyInt(), any(), any(), any(), any()))
                .thenReturn(Mono.empty());
        when(summaryWriter.findOwnedActiveNote("note-1", "owner-1"))
                .thenReturn(Mono.just(latest));

        StepVerifier.create(service.saveSummary(new SaveVoiceDiscussionSummaryRequest(
                        "note-1", "待保存总结", "save_request_409"), httpRequest))
                .expectErrorMatches(error -> error instanceof ResponseStatusException status
                        && status.getStatusCode().value() == 409)
                .verify();

        assertEquals("[{\"insert\":\"另一端的新正文\\n\"}]",
                ((TextDataModule) latest.getModules().getFirst()).getContent());
    }

    @Test
    void responseLossRetryAfterLaterBodyEditUsesPersistentRequestId() {
        Note snapshot = note("note-1", "owner-1", "[{\"insert\":\"正文\\n\"}]");
        Note latest = note("note-1", "owner-1", "[{\"insert\":\"保存后又编辑的正文\\n\"}]");
        latest.setVoiceSummarySaveDigests(Map.of(
                "save_request_same", VoiceDiscussionServiceImplV2.summaryDigest("相同总结")));
        when(ownershipService.ownedNote("note-1", httpRequest)).thenReturn(Mono.just(snapshot));
        when(summaryWriter.compareAndSetTextContent(
                any(), any(), any(), anyInt(), any(), any(), any(), any()))
                .thenReturn(Mono.empty());
        when(summaryWriter.findOwnedActiveNote("note-1", "owner-1"))
                .thenReturn(Mono.just(latest));

        StepVerifier.create(service.saveSummary(new SaveVoiceDiscussionSummaryRequest(
                        "note-1", "相同总结", "save_request_same"), httpRequest))
                .assertNext(result -> {
                    assertTrue(result.alreadySaved());
                    assertEquals(latest, result.note());
                })
                .verifyComplete();
    }

    @Test
    void sameSummaryWithDifferentRequestIdDoesNotMaskConcurrentEdit() {
        Note snapshot = note("note-1", "owner-1", "[{\"insert\":\"正文\\n\"}]");
        Note latest = note("note-1", "owner-1", service.appendSummary(
                "[{\"insert\":\"正文\\n\"}]", "相同总结"));
        latest.setVoiceSummarySaveDigests(Map.of(
                "a_different_request_id", VoiceDiscussionServiceImplV2.summaryDigest("相同总结")));
        when(ownershipService.ownedNote("note-1", httpRequest)).thenReturn(Mono.just(snapshot));
        when(summaryWriter.compareAndSetTextContent(
                any(), any(), any(), anyInt(), any(), any(), any(), any()))
                .thenReturn(Mono.empty());
        when(summaryWriter.findOwnedActiveNote("note-1", "owner-1"))
                .thenReturn(Mono.just(latest));

        StepVerifier.create(service.saveSummary(new SaveVoiceDiscussionSummaryRequest(
                        "note-1", "相同总结", "save_request_new"), httpRequest))
                .expectErrorMatches(error -> error instanceof ResponseStatusException status
                        && status.getStatusCode().value() == 409)
                .verify();
    }

    @Test
    void reusedRequestIdWithDifferentSummaryReturns409BeforeWrite() {
        Note note = note("note-1", "owner-1", "[{\"insert\":\"正文\\n\"}]");
        note.setVoiceSummarySaveDigests(Map.of(
                "save_request_reused", VoiceDiscussionServiceImplV2.summaryDigest("旧总结")));
        when(ownershipService.ownedNote("note-1", httpRequest)).thenReturn(Mono.just(note));

        StepVerifier.create(service.saveSummary(new SaveVoiceDiscussionSummaryRequest(
                        "note-1", "不同的新总结", "save_request_reused"), httpRequest))
                .expectErrorMatches(error -> error instanceof ResponseStatusException status
                        && status.getStatusCode().value() == 409)
                .verify();

        verify(summaryWriter, never()).compareAndSetTextContent(
                any(), any(), any(), anyInt(), any(), any(), any(), any());
    }

    private Note note(String id, String ownerId, String content) {
        TextDataModule textModule = new TextDataModule();
        textModule.setModuleId("text-1");
        textModule.setTitle("测试笔记");
        textModule.setContent(content);
        Note note = new Note();
        note.setId(id);
        note.setDeviceId(ownerId);
        note.setTitle("测试笔记");
        note.setModules(List.<NoteModule>of(textModule));
        return note;
    }
}
