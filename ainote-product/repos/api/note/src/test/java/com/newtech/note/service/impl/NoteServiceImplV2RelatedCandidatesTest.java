package com.newtech.note.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtech.note.entity.dto.Note;
import com.newtech.note.repositories.NoteRepository;
import com.newtech.note.service.FileUploadService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

class NoteServiceImplV2RelatedCandidatesTest {

    @Test
    void listsOnlyOwnersActiveUserNonEmptyNotesAndExcludesSource() {
        NoteRepository repository = mock(NoteRepository.class);
        NoteServiceImplV2 service = new NoteServiceImplV2(
                repository, mock(FileUploadService.class), new ObjectMapper());
        Note valid = note("valid", "owner", false, 0, "", "content");
        Note source = note("source", "owner", false, 0, "source", "source");
        Note deleted = note("deleted", "owner", true, 0, "deleted", "deleted");
        Note system = note("system", "owner", false, 1, "system", "system");
        Note foreign = note("foreign", "other", false, 0, "foreign", "foreign");
        Note empty = note("empty", "owner", false, 0, " ", " ");
        when(repository.findByDeviceIdAndDeletedFalseAndDimension("owner", 0)).thenReturn(
                Flux.just(source, valid, deleted, system, foreign, empty));

        StepVerifier.create(service.listRelatedNoteCandidates("owner", "source"))
                .expectNext(valid)
                .verifyComplete();
        verify(repository).findByDeviceIdAndDeletedFalseAndDimension("owner", 0);
    }

    private Note note(String id, String owner, boolean deleted, int dimension,
                      String title, String content) {
        Note note = mock(Note.class);
        when(note.getId()).thenReturn(id);
        when(note.getDeviceId()).thenReturn(owner);
        when(note.isDeleted()).thenReturn(deleted);
        when(note.getDimension()).thenReturn(dimension);
        when(note.getTitle()).thenReturn(title);
        when(note.getContent()).thenReturn(content);
        return note;
    }
}
