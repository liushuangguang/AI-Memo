package com.newtech.note.service.impl;

import com.newtech.note.entity.dto.NoteAnalysisRecord;
import com.newtech.note.repositories.NoteAnalysisRecordRepository;
import com.newtech.note.repositories.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NoteAnalysisRecordServiceImplTest {
    private NoteAnalysisRecordRepository repository;
    private NoteAnalysisRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(NoteAnalysisRecordRepository.class);
        service = new NoteAnalysisRecordServiceImpl(repository, mock(NoteRepository.class));
    }

    @Test
    void reusesLatestOnlyWhenRawSnapshotMatches() {
        NoteAnalysisRecord existing = record("existing", "note", "same", 3);
        when(repository.findByNoteIdAndSnapshotHash(eq("note"), anyString())).thenReturn(Mono.just(existing));

        StepVerifier.create(service.findOrCreateForSnapshot("note", "same"))
                .expectNext(existing)
                .verifyComplete();
        verify(repository, never()).insert(any(NoteAnalysisRecord.class));
    }

    @Test
    void changedBodyCreatesNextVersionWithCurrentSnapshot() {
        NoteAnalysisRecord old = record("old", "note", "before", 1);
        when(repository.findByNoteIdAndSnapshotHash(eq("note"), anyString())).thenReturn(Mono.empty());
        when(repository.getLatestVersion("note")).thenReturn(Mono.just(1));
        when(repository.findByNoteIdAndVersion("note", 1)).thenReturn(Mono.just(old));
        when(repository.allocateNextVersion("note", 1)).thenReturn(Mono.just(2));
        when(repository.insert(any(NoteAnalysisRecord.class))).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0, NoteAnalysisRecord.class)));

        StepVerifier.create(service.findOrCreateForSnapshot("note", "after"))
                .assertNext(created -> {
                    assertThat(created.getId()).isNotBlank();
                    assertThat(created.getRawNote()).isEqualTo("after");
                    assertThat(created.getVersion()).isEqualTo(2);
                })
                .verifyComplete();
        verify(repository, times(1)).insert(any(NoteAnalysisRecord.class));
    }

    @Test
    void concurrentSameSnapshotCreationIsIdempotentWithinServiceInstance() {
        when(repository.findByNoteIdAndSnapshotHash(eq("note"), anyString())).thenReturn(Mono.empty());
        when(repository.getLatestVersion("note")).thenReturn(Mono.empty());
        when(repository.allocateNextVersion("note", 0)).thenReturn(Mono.just(1));
        when(repository.insert(any(NoteAnalysisRecord.class))).thenAnswer(invocation ->
                Mono.delay(Duration.ofMillis(30))
                        .thenReturn(invocation.getArgument(0, NoteAnalysisRecord.class)));

        StepVerifier.create(Flux.range(0, 20)
                        .flatMap(index -> service.findOrCreateForSnapshot("note", "same snapshot"))
                        .map(NoteAnalysisRecord::getId)
                        .collectList())
                .assertNext(ids -> assertThat(ids).hasSize(20).containsOnly(ids.getFirst()))
                .verifyComplete();
        verify(repository, times(1)).insert(any(NoteAnalysisRecord.class));
    }

    private NoteAnalysisRecord record(String id, String noteId, String raw, int version) {
        NoteAnalysisRecord record = new NoteAnalysisRecord();
        record.setId(id);
        record.setNoteId(noteId);
        record.setRawNote(raw);
        record.setVersion(version);
        return record;
    }
}
