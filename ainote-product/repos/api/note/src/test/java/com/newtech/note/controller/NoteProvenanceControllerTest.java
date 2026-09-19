package com.newtech.note.controller;

import com.newtech.note.entity.dto.Note;
import com.newtech.note.entity.dto.NoteTheme;
import com.newtech.note.entity.dto.NoteThemeMergeHistory;
import com.newtech.note.security.NoteOwnershipService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class NoteProvenanceControllerTest {
    private final NoteOwnershipService ownership = mock(NoteOwnershipService.class);
    private final ReactiveMongoTemplate mongo = mock(ReactiveMongoTemplate.class);
    private final NoteProvenanceController controller = new NoteProvenanceController(ownership, mongo);
    private final MockServerHttpRequest request = MockServerHttpRequest.get("/v2/note/merged/merge-history").build();

    @Test void identityFailureNeverQueriesThemes() {
        when(ownership.ownedNote("merged", request)).thenReturn(Mono.error(new IllegalStateException("denied")));
        StepVerifier.create(controller.history("merged", request)).expectErrorMessage("denied").verify();
        verifyNoInteractions(mongo);
    }

    @Test void deletedOrUnassociatedNoteHasNoHistory() {
        var note = new Note();
        when(ownership.ownedNote("merged", request)).thenReturn(Mono.just(note));
        assertThat(controller.history("merged", request).block().getData()).isEmpty();
        note.setNoteThemeIds(List.of("theme-a"));
        note.setDeleted(true);
        assertThat(controller.history("merged", request).block().getData()).isEmpty();
        verifyNoInteractions(mongo);
    }

    @Test void queryIsOwnerThemeAndMergedNoteScopedAndUnrelatedHistoryIsExcluded() {
        var note = new Note(); note.setId("merged"); note.setDeviceId("guest-a");
        note.setNoteThemeIds(List.of("theme-a"));
        var wanted = new NoteThemeMergeHistory(); wanted.setMergedNoteId("merged");
        var other = new NoteThemeMergeHistory(); other.setMergedNoteId("other-note");
        var theme = new NoteTheme(); theme.setMergeHistory(List.of(other, wanted));
        when(ownership.ownedNote("merged", request)).thenReturn(Mono.just(note));
        when(mongo.find(any(Query.class), eq(NoteTheme.class))).thenReturn(Flux.just(theme, new NoteTheme()));
        assertThat(controller.history("merged", request).block().getData()).containsExactly(wanted);
        var query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(NoteTheme.class));
        assertThat(query.getValue().getQueryObject().toJson()).contains("theme-a", "guest-a", "merged", "deleted");
        assertThat(query.getValue().getQueryObject().get("deviceId")).isEqualTo("guest-a");
        assertThat(query.getValue().getQueryObject().get("mergeHistory.mergedNoteId")).isEqualTo("merged");
        assertThat(query.getValue().getLimit()).isEqualTo(20);
    }
}
