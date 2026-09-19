package com.newtech.note.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtech.note.entity.dto.Note;
import com.newtech.note.entity.request.noteModule.AddNoteModuleRequest;
import com.newtech.note.repositories.NoteRepository;
import com.newtech.note.service.FileUploadService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

class NoteModuleAtomicSaveTest {
    @Test
    void bodyUpdateFromOldSnapshotPreservesAnInterleavedAppendedModule() {
        var server = new de.bwaldvogel.mongo.MongoServer(new de.bwaldvogel.mongo.backend.memory.MemoryBackend());
        server.bind("127.0.0.1", 0);
        try (var client = com.mongodb.reactivestreams.client.MongoClients.create(server.getConnectionString())) {
            var template = new ReactiveMongoTemplate(client, "module-concurrency");
            var repository = mock(NoteRepository.class);
            var service = new NoteServiceImplV2(repository, mock(FileUploadService.class), new ObjectMapper());
            ReflectionTestUtils.setField(service, "moduleTemplate", template);
            var text = new com.newtech.note.entity.dto.noteModules.TextDataModule();
            text.setModuleId("text-1"); text.setTitle("旧标题"); text.setContent("原始正文");
            var note = new Note(); note.setId("note-1"); note.setDeviceId("owner");
            note.setModules(new java.util.ArrayList<>(java.util.List.of(text)));
            template.insert(note).block();
            var image = new com.newtech.note.entity.dto.noteModules.AIPictureModule();
            image.setModuleId("concurrent-image"); image.setImageUrl("https://example.test/a.png");
            when(repository.findById("note-1")).thenAnswer(ignored ->
                template.findById("note-1", Note.class).flatMap(snapshot ->
                    template.updateFirst(Query.query(org.springframework.data.mongodb.core.query.Criteria.where("_id").is("note-1")),
                        new Update().push("modules", image), Note.class).thenReturn(snapshot)));
            var payload = new com.newtech.note.entity.request.noteModule.UpdateNoteModuleRequest.UpdateTextDataModuleRequest();
            payload.setContent("更新后的正文"); payload.setTitle("新标题");
            var request = new com.newtech.note.entity.request.noteModule.UpdateNoteModuleRequest();
            request.setId("note-1"); request.setModuleId("text-1"); request.setModule(payload);
            var result = service.updateNoteModule("owner", request).block();
            assertThat(result.isSuccess()).isTrue();
            var stored = template.findById("note-1", Note.class).block();
            assertThat(stored.getContent()).isEqualTo("更新后的正文");
            assertThat(stored.getModules()).hasSize(2);
            assertThat(stored.getModules().get(1).getModuleId()).isEqualTo("concurrent-image");
            verify(repository, never()).save(any());
        } finally { server.shutdown(); }
    }

    @Test
    void repeatedModuleUsesStableIdAndOnlyAtomicAppendNeverWholeNoteSave() {
        var repository = mock(NoteRepository.class);
        var template = mock(ReactiveMongoTemplate.class);
        var service = new NoteServiceImplV2(repository, mock(FileUploadService.class), new ObjectMapper());
        ReflectionTestUtils.setField(service, "moduleTemplate", template);
        var note = new Note();
        note.setId("n");
        note.setDeviceId("owner");
        when(repository.findById("n")).thenReturn(Mono.just(note));
        when(template.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Note.class)))
                .thenReturn(Mono.just(note), Mono.empty());
        var payload = new AddNoteModuleRequest.CreateAIPictureModuleRequest();
        payload.setImageUrl("https://example.test/real.png");
        payload.setDescription("用户确认配图");
        var request = new AddNoteModuleRequest();
        request.setId("n"); request.setModule(payload);
        assertThat(service.addNoteModule("owner", request).block().isSuccess()).isTrue();
        assertThat(service.addNoteModule("owner", request).block().isSuccess()).isTrue();
        var queries = ArgumentCaptor.forClass(Query.class);
        var updates = ArgumentCaptor.forClass(Update.class);
        verify(template, times(2)).findAndModify(queries.capture(), updates.capture(), any(FindAndModifyOptions.class), eq(Note.class));
        assertThat(queries.getAllValues().get(0).getQueryObject()).isEqualTo(queries.getAllValues().get(1).getQueryObject());
        assertThat(queries.getValue().getQueryObject().toJson()).contains("owner", "deleted", "saved-module-");
        var update = updates.getValue().getUpdateObject();
        assertThat(update.keySet()).containsExactlyInAnyOrder("$push", "$set");
        assertThat(((org.bson.Document) update.get("$set")).keySet()).containsExactly("updatedAt");
        verify(repository, never()).save(any());
    }
}
