package com.newtech.note.service.impl;

import com.newtech.note.entity.dto.NoteAssistRecord;
import com.newtech.note.entity.request.CreateNoteAssistRecordRequest;
import com.newtech.note.entity.request.UpdateNoteAssistRecordRequest;
import com.newtech.note.repositories.NoteAssistRecordRepository;
import com.newtech.note.repositories.NoteRepository;
import com.newtech.note.service.NoteAssistRecordService;
import com.newtech.note.util.SnowflakeIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
public class NoteAssistRecordServiceImpl implements NoteAssistRecordService {

    private static final Logger logger = LogManager.getLogger(NoteAssistRecordServiceImpl.class);
    private final NoteAssistRecordRepository noteAssistRecordRepository;
    private final NoteRepository noteRepository;

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public NoteAssistRecordServiceImpl(NoteAssistRecordRepository noteAssistRecordRepository, NoteRepository noteRepository) {
        this.noteAssistRecordRepository = noteAssistRecordRepository;
        this.noteRepository = noteRepository;
        this.snowflakeIdGenerator = SnowflakeIdGenerator.getInstance();
    }

    @Override
    public Mono<NoteAssistRecord> findById(String recordId) {
        return noteAssistRecordRepository.findById(recordId);
    }

    @Override
    public Mono<NoteAssistRecord> update(UpdateNoteAssistRecordRequest request) {
        return noteAssistRecordRepository
                .findById(request.getId())
                .flatMap(noteAssistRecord -> {
                    replenishNoteAssistRecord(request, noteAssistRecord);
                    return noteAssistRecordRepository.save(noteAssistRecord);
                });
    }

    private static void replenishNoteAssistRecord(UpdateNoteAssistRecordRequest request,
                                                  NoteAssistRecord noteAssistRecord) {
        logger.info("Updating note assist record");
        if (StringUtils.isNotBlank(request.getOverallReview())) {
            noteAssistRecord.setOverallReview(request.getOverallReview());
        }
        if (request.getRewrittenContent() != null) {
            noteAssistRecord.setRewrittenContent(request.getRewrittenContent());
            noteAssistRecord.setAssistContent(request.getAssistDirection());
            noteAssistRecord.setSelectedContent(request.getSelectedContent());
        }
        noteAssistRecord.setUpdatedAt(LocalDateTime.now());
    }

    @Override
    public Mono<NoteAssistRecord> create(CreateNoteAssistRecordRequest request) {
        return noteRepository.findById(request.getNoteId())
                .flatMap(note -> noteAssistRecordRepository.getLatestVersion(request.getNoteId())
                        .flatMap(latestVersion -> {
                            NoteAssistRecord noteAssistRecord = new NoteAssistRecord();
                            noteAssistRecord.setId(snowflakeIdGenerator.nextFullId(NoteAssistRecord.class));
                            noteAssistRecord.setNoteId(request.getNoteId());
                            noteAssistRecord.setRawNote(note.getContent());
                            noteAssistRecord.setVersion(latestVersion + 1);
                            noteAssistRecord.setCreatedAt(LocalDateTime.now());
                            noteAssistRecord.setUpdatedAt(LocalDateTime.now());
                            return noteAssistRecordRepository.insert(noteAssistRecord);
                        }).switchIfEmpty(Mono.defer(() -> {
                            NoteAssistRecord noteAssistRecord = new NoteAssistRecord();
                            noteAssistRecord.setId(snowflakeIdGenerator.nextFullId(NoteAssistRecord.class));
                            noteAssistRecord.setNoteId(request.getNoteId());
                            noteAssistRecord.setRawNote(note.getContent());
                            noteAssistRecord.setVersion(1);
                            noteAssistRecord.setCreatedAt(LocalDateTime.now());
                            noteAssistRecord.setUpdatedAt(LocalDateTime.now());
                            return noteAssistRecordRepository.insert(noteAssistRecord);
                        })));

    }

    @Override
    public Mono<NoteAssistRecord> latestAssist(String noteId) {
        return noteAssistRecordRepository.getLatestVersion(noteId)
                .flatMap(version -> noteAssistRecordRepository.findByNoteIdAndVersion(noteId,
                        version));
    }

    @Override
    public Flux<NoteAssistRecord> allAssistRecords(String noteId) {
        return noteAssistRecordRepository.findByNoteId(noteId);
    }

    @Override
    public Mono<NoteAssistRecord> specificAssist(String noteId, int version) {
        return noteAssistRecordRepository.findByNoteIdAndVersion(noteId, version);
    }
}
