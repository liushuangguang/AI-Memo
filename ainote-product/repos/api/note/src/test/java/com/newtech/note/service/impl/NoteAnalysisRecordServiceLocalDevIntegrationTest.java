package com.newtech.note.service.impl;

import com.newtech.note.entity.dto.NoteAnalysisRecord;
import com.newtech.note.entity.dto.Note;
import com.newtech.note.entity.dto.noteModules.TextDataModule;
import com.newtech.note.entity.request.CreateNoteAnalysisRecordRequest;
import com.newtech.note.repositories.NoteRepository;
import com.newtech.note.service.NoteAnalysisRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "app.local-dev.mongodb.port=27019",
                "app.local-dev.mongodb.file=target/test-local-mongo-${random.uuid}.mv",
                "spring.data.mongodb.uri=mongodb://127.0.0.1:27019/note-record-test",
                "spring.data.mongodb.database=note-record-test",
                "jwt.key=test-local-dev-jwt-key-not-for-production-2026"
        })
@ActiveProfiles("local-dev")
class NoteAnalysisRecordServiceLocalDevIntegrationTest {

    @Autowired
    private NoteAnalysisRecordService service;

    @Autowired
    private NoteRepository noteRepository;

    @Test
    void createsAndReusesSnapshotThroughTheSpringProxyWithoutAResourceMismatch() {
        Note note = new Note();
        note.setId("integration-note");
        TextDataModule textModule = new TextDataModule();
        textModule.setContent("integration snapshot");
        note.setModules(List.of(textModule));
        noteRepository.insert(note).block();

        CreateNoteAnalysisRecordRequest request = CreateNoteAnalysisRecordRequest.builder()
                .noteId(note.getId())
                .build();
        NoteAnalysisRecord first = service
                .create(request)
                .block();
        NoteAnalysisRecord second = service
                .create(request)
                .block();

        assertThat(first).isNotNull();
        assertThat(first.getVersion()).isEqualTo(1);
        assertThat(first.getRawNote()).isEqualTo("integration snapshot");
        assertThat(second).isNotNull();
        assertThat(second.getId()).isEqualTo(first.getId());
    }
}
