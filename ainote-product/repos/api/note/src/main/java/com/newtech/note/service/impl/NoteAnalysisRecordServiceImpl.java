package com.newtech.note.service.impl;

import com.newtech.note.entity.dto.NoteAnalysisRecord;
import com.newtech.note.entity.request.CreateNoteAnalysisRecordRequest;
import com.newtech.note.entity.request.UpdateNoteAnalysisRecordRequest;
import com.newtech.note.repositories.NoteAnalysisRecordRepository;
import com.newtech.note.repositories.NoteRepository;
import com.newtech.note.service.NoteAnalysisRecordService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class NoteAnalysisRecordServiceImpl implements NoteAnalysisRecordService {
    private static final Logger logger = LogManager.getLogger(NoteAnalysisRecordServiceImpl.class);
    private final NoteAnalysisRecordRepository noteAnalysisRecordRepository;
    private final NoteRepository noteRepository;

    private final ConcurrentMap<String, Mono<NoteAnalysisRecord>> snapshotCreates = new ConcurrentHashMap<>();

    public NoteAnalysisRecordServiceImpl(NoteAnalysisRecordRepository noteAnalysisRecordRepository,
                                         NoteRepository noteRepository) {
        this.noteAnalysisRecordRepository = noteAnalysisRecordRepository;
        this.noteRepository = noteRepository;
    }

    @Override
    public Mono<NoteAnalysisRecord> create(CreateNoteAnalysisRecordRequest request) {
        return noteRepository.findById(request.getNoteId())
                .flatMap(note -> findOrCreateForSnapshot(request.getNoteId(), note.getContent()));

    }

    @Override
    public Mono<NoteAnalysisRecord> findOrCreateForSnapshot(String noteId, String rawNote) {
        if (StringUtils.isBlank(noteId) || StringUtils.isBlank(rawNote)) {
            return Mono.error(new IllegalArgumentException("noteId and rawNote are required"));
        }
        String snapshotHash = snapshotKey(noteId, rawNote);
        return noteAnalysisRecordRepository.findByNoteIdAndSnapshotHash(noteId, snapshotHash)
                .switchIfEmpty(Mono.defer(() -> latestAnalysis(noteId)
                        .filter(record -> java.util.Objects.equals(rawNote, record.getRawNote()))))
                .switchIfEmpty(Mono.defer(() -> {
                    Mono<NoteAnalysisRecord> shared = snapshotCreates.computeIfAbsent(snapshotHash, ignored ->
                            noteAnalysisRecordRepository.findByNoteIdAndSnapshotHash(noteId, snapshotHash)
                                    .switchIfEmpty(Mono.defer(() -> createSnapshot(noteId, rawNote, snapshotHash)))
                                    .cache());
                    return shared.doFinally(signal -> snapshotCreates.remove(snapshotHash, shared));
                }));
    }

    private Mono<NoteAnalysisRecord> createSnapshot(String noteId, String rawNote, String snapshotHash) {
        return noteAnalysisRecordRepository.getLatestVersion(noteId)
                .defaultIfEmpty(0)
                .flatMap(latestVersion -> noteAnalysisRecordRepository.allocateNextVersion(noteId, latestVersion))
                .flatMap(nextVersion -> {
                    NoteAnalysisRecord record = new NoteAnalysisRecord();
                    record.setId("snapshot-" + snapshotHash);
                    record.setNoteId(noteId);
                    record.setRawNote(rawNote);
                    record.setSnapshotHash(snapshotHash);
                    record.setVersion(nextVersion);
                    record.setCreatedAt(LocalDateTime.now());
                    record.setUpdatedAt(LocalDateTime.now());
                    return noteAnalysisRecordRepository.insert(record)
                            .onErrorResume(DuplicateKeyException.class,
                                    failure -> noteAnalysisRecordRepository.findById(record.getId())
                                            .switchIfEmpty(Mono.error(failure)));
                });
    }

    private String snapshotKey(String noteId, String rawNote) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(noteId.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            return HexFormat.of().formatHex(digest.digest(rawNote.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }

    }

    @Override
    public Mono<NoteAnalysisRecord> update(UpdateNoteAnalysisRecordRequest request) {
        logger.info("Updating note analysis record");
        return noteAnalysisRecordRepository.patch(request.getId(), request);
    }

    @Override
    public Mono<NoteAnalysisRecord> latestAnalysis(String noteId) {
        return noteAnalysisRecordRepository.getLatestVersion(noteId)
                .flatMap(version -> noteAnalysisRecordRepository.findByNoteIdAndVersion(noteId,
                        version));
    }

    @Override
    public Mono<NoteAnalysisRecord> findById(String id) {
        return noteAnalysisRecordRepository.findById(id);
    }

    @Override
    public Flux<NoteAnalysisRecord> allAnalysisRecords(String noteId) {
        return noteAnalysisRecordRepository.findByNoteId(noteId);
    }

    @Override
    public Mono<NoteAnalysisRecord> specificAnalysis(String noteId, int version) {
        return noteAnalysisRecordRepository.findByNoteIdAndVersion(noteId, version);
    }

}
