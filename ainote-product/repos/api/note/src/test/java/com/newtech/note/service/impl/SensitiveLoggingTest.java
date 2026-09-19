package com.newtech.note.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtech.note.entity.dto.Note;
import com.newtech.note.entity.request.CreateNoteRequest;
import com.newtech.note.entity.request.CreateNoteAnalysisRequest;
import com.newtech.note.entity.request.CreateBacklogRequest;
import com.newtech.note.entity.dto.NoteAnalysis;
import com.newtech.note.entity.dto.Backlog;
import com.newtech.note.repositories.NoteAnalysisRepository;
import com.newtech.note.repositories.BacklogRepository;
import com.newtech.note.service.NoteAnalysisHistoryService;
import com.newtech.note.repositories.NoteRepository;
import com.newtech.note.service.FileUploadService;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SensitiveLoggingTest {

    @Test
    void noteBodyAndTitleMarkersAreNotWrittenToApplicationLogs() {
        NoteRepository repository = mock(NoteRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        NoteServiceImplV2 service = new NoteServiceImplV2(
                repository, mock(FileUploadService.class), new ObjectMapper());
        CreateNoteRequest request = new CreateNoteRequest();
        request.setTitle("SENSITIVE_TITLE_MARKER");
        request.setContent("SENSITIVE_BODY_MARKER");

        CapturingAppender appender = new CapturingAppender();
        LoggerContext context = LoggerContext.getContext(false);
        Configuration configuration = context.getConfiguration();
        configuration.getRootLogger().addAppender(appender, Level.ALL, null);
        appender.start();
        context.updateLoggers();
        try {
            service.createNote("owner", request).block();
        } finally {
            configuration.getRootLogger().removeAppender(appender.getName());
            appender.stop();
            context.updateLoggers();
        }

        assertThat(String.join("\n", appender.messages))
                .doesNotContain("SENSITIVE_TITLE_MARKER", "SENSITIVE_BODY_MARKER");
    }

    @Test
    void legacyNoteAndBacklogMarkersAreNotWrittenToApplicationLogs() {
        NoteAnalysisRepository legacyRepository = mock(NoteAnalysisRepository.class);
        when(legacyRepository.insert(any(NoteAnalysis.class))).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0, NoteAnalysis.class)));
        NoteServiceImpl legacyNoteService = new NoteServiceImpl(
                legacyRepository, mock(NoteAnalysisHistoryService.class),
                mock(FileUploadService.class), new ObjectMapper());

        BacklogRepository backlogRepository = mock(BacklogRepository.class);
        when(backlogRepository.save(any(Backlog.class))).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0, Backlog.class)));
        BacklogServiceImpl backlogService = new BacklogServiceImpl(backlogRepository);

        CapturingAppender appender = installAppender();
        try {
            legacyNoteService.createNote("owner", new CreateNoteAnalysisRequest(
                    "LEGACY_TITLE_MARKER", "LEGACY_BODY_MARKER", 0, 0)).block();
            CreateBacklogRequest backlog = new CreateBacklogRequest();
            backlog.setContent("BACKLOG_CONTENT_MARKER");
            backlog.setDescription("BACKLOG_DESCRIPTION_MARKER");
            backlogService.createBacklog("owner", backlog).block();
        } finally {
            removeAppender(appender);
        }

        assertThat(String.join("\n", appender.messages)).doesNotContain(
                "LEGACY_TITLE_MARKER", "LEGACY_BODY_MARKER",
                "BACKLOG_CONTENT_MARKER", "BACKLOG_DESCRIPTION_MARKER");
    }

    private CapturingAppender installAppender() {
        CapturingAppender appender = new CapturingAppender();
        LoggerContext context = LoggerContext.getContext(false);
        Configuration configuration = context.getConfiguration();
        configuration.getRootLogger().addAppender(appender, Level.ALL, null);
        appender.start();
        context.updateLoggers();
        return appender;
    }

    private void removeAppender(CapturingAppender appender) {
        LoggerContext context = LoggerContext.getContext(false);
        Configuration configuration = context.getConfiguration();
        configuration.getRootLogger().removeAppender(appender.getName());
        appender.stop();
        context.updateLoggers();
    }

    private static final class CapturingAppender extends AbstractAppender {
        private final List<String> messages = new CopyOnWriteArrayList<>();

        private CapturingAppender() {
            super("sensitive-log-capture", null, PatternLayout.createDefaultLayout(),
                    false, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            messages.add(event.getMessage().getFormattedMessage());
        }
    }
}
