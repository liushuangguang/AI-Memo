package com.newtech.note.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtech.note.client.DeepSeekClient;
import com.newtech.note.common.BusinessException;
import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.entity.dto.NoteAnalysisRecord;
import com.newtech.note.entity.request.AnalyzeNoteRequestV2;
import com.newtech.note.security.NoteOwnershipService;
import com.newtech.note.service.NoteAnalysisServiceV2;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class NoteReflectionControllerTest {
    private final NoteOwnershipService ownership = mock(NoteOwnershipService.class);
    private final NoteAnalysisServiceV2 analysis = mock(NoteAnalysisServiceV2.class);
    private final DeepSeekClient ai = mock(DeepSeekClient.class);
    private final NoteReflectionController controller = new NoteReflectionController(ownership, analysis, ai, new ObjectMapper());
    private final MockServerHttpRequest http = MockServerHttpRequest.post("/v2/note/analysis/reflection").build();
    private final AnalyzeNoteRequestV2 request = new AnalyzeNoteRequestV2("record-a");

    @Test void deniesCrossOwnerBeforeAnyRetrievalOrAiCall() {
        when(ownership.ownedAnalysisRecord("record-a", http)).thenReturn(Mono.error(new BusinessException("FORBIDDEN", "Denied")));
        StepVerifier.create(controller.reflection(request, http)).expectError(BusinessException.class).verify();
        verifyNoInteractions(analysis, ai);
    }

    @Test void noHistoryMeansNoInventedReflectionAndNoProviderCall() {
        var record = new NoteAnalysisRecord();
        record.setId("record-a");
        when(ownership.ownedAnalysisRecord("record-a", http)).thenReturn(Mono.just(record));
        when(analysis.relatedNotes(request, http)).thenReturn(Mono.just(NoteBaseResponse.success(List.of())));
        var result = controller.reflection(request, http).block();
        assertNotNull(result);
        assertEquals(List.of(), result.getData().get("sources"));
        assertTrue(result.getData().get("summary").toString().contains("暂无"));
        verifyNoInteractions(ai);
    }
}
