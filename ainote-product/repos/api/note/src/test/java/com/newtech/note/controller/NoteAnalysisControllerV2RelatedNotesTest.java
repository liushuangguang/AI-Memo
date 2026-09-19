package com.newtech.note.controller;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.entity.request.AnalyzeNoteRequestV2;
import com.newtech.note.service.NoteAnalysisServiceV2;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class NoteAnalysisControllerV2RelatedNotesTest {

    @Test
    void exposesRelatedNotesPostRouteAndPassesServerRequest() throws Exception {
        Method method = NoteAnalysisControllerV2.class.getMethod(
                "relatedNotes", AnalyzeNoteRequestV2.class,
                org.springframework.http.server.reactive.ServerHttpRequest.class);
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        assertThat(mapping.value()).containsExactly("/relatedNotes");

        NoteAnalysisServiceV2 service = mock(NoteAnalysisServiceV2.class);
        NoteAnalysisControllerV2 controller = new NoteAnalysisControllerV2(service);
        AnalyzeNoteRequestV2 body = new AnalyzeNoteRequestV2("record");
        var request = MockServerHttpRequest.post("/v2/note/analysis/relatedNotes").build();
        when(service.relatedNotes(body, request)).thenReturn(Mono.just(NoteBaseResponse.success(List.of())));

        StepVerifier.create(controller.relatedNotes(body, request))
                .assertNext(response -> assertThat(response.getData()).isEmpty())
                .verifyComplete();
        verify(service).relatedNotes(body, request);
    }
}
