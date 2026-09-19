package com.newtech.note.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtech.note.common.BusinessException;
import com.newtech.note.repositories.AudioSessionRepository;
import com.newtech.note.repositories.NoteAnalysisHistoryRepository;
import com.newtech.note.repositories.NoteAnalysisRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class AudioSessionServiceImplCozeTest {

    @Test
    void providerBusinessErrorFromHttp200IsTyped() {
        AudioSessionServiceImpl service = serviceReturning("{\"code\":\"4001\",\"msg\":\"failed\"}");

        assertBusinessError(service.noteDiscussAudioAiSummary(new ObjectMapper().createObjectNode()), "COZE_4001");
    }

    @Test
    void malformedProviderResponseIsTyped() {
        AudioSessionServiceImpl service = serviceReturning("not-json");

        assertBusinessError(
                service.noteDiscussAudioAiSummary(new ObjectMapper().createObjectNode()),
                "COZE_MALFORMED_RESPONSE");
    }

    @Test
    void successfulResponseWithoutAnswerIsNotAnEmptySuccess() {
        AudioSessionServiceImpl service = serviceReturning("{\"code\":\"0\",\"messages\":[]}");

        assertBusinessError(
                service.noteDiscussAudioAiSummary(new ObjectMapper().createObjectNode()),
                "COZE_EMPTY_RESPONSE");
    }

    @Test
    void neverCompletingAudioProviderCallHasTypedTimeout() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.never())
                .build();
        AudioSessionServiceImpl service = new AudioSessionServiceImpl(
                mock(AudioSessionRepository.class),
                mock(NoteAnalysisRepository.class),
                mock(NoteAnalysisHistoryRepository.class),
                webClient,
                mock(DatabaseClient.class),
                new ObjectMapper(),
                "test-key",
                "https://coze.test/chat",
                1);

        StepVerifier.create(service.noteDiscussAudioAiSummary(new ObjectMapper().createObjectNode()))
                .expectErrorSatisfies(error -> {
                    BusinessException failure = assertInstanceOf(BusinessException.class, error);
                    assertEquals("COZE_TIMEOUT", failure.getCode());
                })
                .verify(java.time.Duration.ofSeconds(3));
    }

    private AudioSessionServiceImpl serviceReturning(String json) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(json)
                        .build()))
                .build();
        return new AudioSessionServiceImpl(
                mock(AudioSessionRepository.class),
                mock(NoteAnalysisRepository.class),
                mock(NoteAnalysisHistoryRepository.class),
                webClient,
                mock(DatabaseClient.class),
                new ObjectMapper(),
                "test-key",
                "https://coze.test/chat");
    }

    private void assertBusinessError(Mono<?> publisher, String code) {
        StepVerifier.create(publisher)
                .expectErrorSatisfies(error -> {
                    BusinessException failure = assertInstanceOf(BusinessException.class, error);
                    assertEquals(code, failure.getCode());
                })
                .verify();
    }
}
