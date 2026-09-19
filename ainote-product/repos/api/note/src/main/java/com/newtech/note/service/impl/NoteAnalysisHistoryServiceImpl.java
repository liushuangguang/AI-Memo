package com.newtech.note.service.impl;

import com.newtech.note.common.enumeration.NoteAnalysisType;
import com.newtech.note.entity.dto.NoteAnalysisHistory;
import com.newtech.note.entity.request.CreateNoteAnalysisHistoryRequest;
import com.newtech.note.entity.request.UpdateNoteAnalysisHistoryRequest;
import com.newtech.note.repositories.NoteAnalysisHistoryRepository;
import com.newtech.note.repositories.NoteAnalysisRepository;
import com.newtech.note.service.NoteAnalysisHistoryService;
import com.newtech.note.util.SnowflakeIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
public class NoteAnalysisHistoryServiceImpl implements NoteAnalysisHistoryService {
    private static final Logger logger = LogManager.getLogger(NoteAnalysisHistoryServiceImpl.class);
    private final NoteAnalysisHistoryRepository noteAnalysisHistoryRepository;
    private final NoteAnalysisRepository noteAnalysisRepository;

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public NoteAnalysisHistoryServiceImpl(NoteAnalysisHistoryRepository noteAnalysisHistoryRepository,
                                          NoteAnalysisRepository noteAnalysisRepository) {
        this.noteAnalysisHistoryRepository = noteAnalysisHistoryRepository;
        this.noteAnalysisRepository = noteAnalysisRepository;
        this.snowflakeIdGenerator = SnowflakeIdGenerator.getInstance();
    }

    private static void replenishNoteAnalysisHistory(UpdateNoteAnalysisHistoryRequest request,
                                                     NoteAnalysisHistory noteAnalysisHistory) {
        noteAnalysisHistory.setNoteAnalysisId(request.getNoteAnalysisId());
        if (request.getAnalysisType() == NoteAnalysisType.ORGANIZE_WITH_ONE_CLICK.getType()) {
            logger.info("Updating note analysis history");
            if (StringUtils.isNotBlank(request.getOrganizedNoteText())) {
                noteAnalysisHistory.setOrganizedNoteText(request.getOrganizedNoteText());
            }
            if (StringUtils.isNotBlank(request.getCategorizedNotes())) {
                noteAnalysisHistory.setCategorizedNotes(request.getCategorizedNotes());
            }
            if (StringUtils.isNotBlank(request.getRelatedNotes())) {
                noteAnalysisHistory.setRelatedNotes(request.getRelatedNotes());
            }
            if (StringUtils.isNotBlank(request.getRelatedLinks())) {
                noteAnalysisHistory.setRelatedLinks(request.getRelatedLinks());
            }
            if (StringUtils.isNotBlank(request.getProductRecommendations())) {
                noteAnalysisHistory.setProductRecommendations(request.getProductRecommendations());
            }
            if (StringUtils.isNotBlank(request.getImageLink())) {
                noteAnalysisHistory.setImageLink(request.getImageLink());
            }
            if (StringUtils.isNotBlank(request.getRelationalRelatedInfo())) {
                noteAnalysisHistory.setRelationalRelatedInfo(request.getRelationalRelatedInfo());
            }
            if (StringUtils.isNotBlank(request.getRelationalFieldInfo())) {
                noteAnalysisHistory.setRelationalFieldInfo(request.getRelationalFieldInfo());
            }
            String talkSnapshot = request.getTalkSnapshot();
            if (talkSnapshot != null) {
                noteAnalysisHistory.setTalkSnapshot(talkSnapshot);
            }
        } else if (request.getAnalysisType() == NoteAnalysisType.ASSIST_CONTENT.getType()) {
            if (StringUtils.isNotBlank(request.getAssistedContent())) {
                noteAnalysisHistory.setAssistedContent(request.getAssistedContent());
            }
        } else if (request.getAnalysisType() == NoteAnalysisType.DISCUSS_BY_VOICE.getType()) {
            String talkSnapshot = request.getTalkSnapshot();
            if (talkSnapshot != null) {
                noteAnalysisHistory.setTalkSnapshot(talkSnapshot);
            }
        }
        noteAnalysisHistory.setUpdatedAt(LocalDateTime.now());

    }

    @Override
    @Transactional
    public Mono<NoteAnalysisHistory> create(CreateNoteAnalysisHistoryRequest request) {
        return noteAnalysisRepository.findById(request.getNoteAnalysisId())
                .flatMap(noteAnalysis -> noteAnalysisHistoryRepository.getLatestVersion(request.getNoteAnalysisId())
                        .flatMap(latestVersion -> {
                            NoteAnalysisHistory noteAnalysisHistory = new NoteAnalysisHistory();
                            noteAnalysisHistory.setId(snowflakeIdGenerator.nextId(NoteAnalysisHistory.class));
                            noteAnalysisHistory.setNoteAnalysisId(request.getNoteAnalysisId());
                            noteAnalysisHistory.setRawNote(noteAnalysis.getNoteAnalysisContent());
                            noteAnalysisHistory.setVersion(latestVersion + 1);
                            noteAnalysisHistory.setAnalysisType(request.getAnalysisType());
                            noteAnalysisHistory.setCreatedAt(LocalDateTime.now());
                            noteAnalysisHistory.setUpdatedAt(LocalDateTime.now());
                            return noteAnalysisHistoryRepository.insert(noteAnalysisHistory);
                        }).switchIfEmpty(Mono.defer(() -> {
                            NoteAnalysisHistory noteAnalysisHistory = new NoteAnalysisHistory();
                            noteAnalysisHistory.setId(snowflakeIdGenerator.nextId(NoteAnalysisHistory.class));
                            noteAnalysisHistory.setNoteAnalysisId(request.getNoteAnalysisId());
                            noteAnalysisHistory.setRawNote(noteAnalysis.getNoteAnalysisContent());
                            noteAnalysisHistory.setVersion(1);
                            noteAnalysisHistory.setAnalysisType(request.getAnalysisType());
                            noteAnalysisHistory.setCreatedAt(LocalDateTime.now());
                            noteAnalysisHistory.setUpdatedAt(LocalDateTime.now());
                            return noteAnalysisHistoryRepository.insert(noteAnalysisHistory);
                        })));

    }

    @Override
    public Mono<NoteAnalysisHistory> update(UpdateNoteAnalysisHistoryRequest request) {
        return noteAnalysisHistoryRepository
                .findByNoteAnalysisIdAndVersion(request.getNoteAnalysisId(), request.getAnalysisType(), request.getVersion())
                .flatMap(noteAnalysisHistory -> {
                    replenishNoteAnalysisHistory(request, noteAnalysisHistory);
                    return noteAnalysisHistoryRepository.save(noteAnalysisHistory);
                });
    }

    @Override
    public Mono<NoteAnalysisHistory> latestAnalysis(long noteAnalysisId, int analysisType) {
        return noteAnalysisHistoryRepository.getLatestVersion(noteAnalysisId)
                .flatMap(version -> noteAnalysisHistoryRepository.findByNoteAnalysisIdAndVersion(noteAnalysisId,
                        version, analysisType));
    }

    @Override
    public Mono<NoteAnalysisHistory> findByNoteAnalysisIdAndVersion(long noteAnalysisId, int version,
                                                                    int analysisType) {
        return noteAnalysisHistoryRepository.findByNoteAnalysisIdAndVersion(noteAnalysisId, version, analysisType);
    }

    @Override
    public Flux<NoteAnalysisHistory> allAnalysisHistories(long noteAnalysisId) {
        return noteAnalysisHistoryRepository.findByNoteAnalysisId(noteAnalysisId);
    }

    @Override
    public Mono<NoteAnalysisHistory> specificAnalysis(long noteAnalysisId, int version, int analysisType) {
        return noteAnalysisHistoryRepository.findByNoteAnalysisIdAndVersion(noteAnalysisId, version, analysisType);
    }

}
