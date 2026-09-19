package com.newtech.note.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.newtech.note.client.ImageRecognitionClient;
import com.newtech.note.client.RecognizedTextOrganizer;
import com.newtech.note.common.BusinessException;
import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.common.NotePageData;
import com.newtech.note.common.enumeration.RecommendationItemType;
import com.newtech.note.entity.dto.Note;
import com.newtech.note.entity.dto.noteLogs.NoteReplaceTextModuleLog;
import com.newtech.note.entity.dto.noteModules.*;
import com.newtech.note.entity.dto.noteModules.items.*;
import com.newtech.note.entity.filter.NoteFilterV2;
import com.newtech.note.entity.request.CreateNoteRequest;
import com.newtech.note.entity.request.SearchNoteRequest;
import com.newtech.note.entity.request.UpdateNoteRequest;
import com.newtech.note.entity.request.noteModule.*;
import com.newtech.note.repositories.NoteRepository;
import com.newtech.note.service.FileUploadService;
import com.newtech.note.service.NoteServiceV2;
import com.newtech.note.service.PrivateCaptureImageService;
import com.newtech.note.util.SegmentUtil;
import com.newtech.note.util.SnowflakeIdGenerator;
import io.micrometer.common.util.StringUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class NoteServiceImplV2 implements NoteServiceV2 {
    private static final Logger logger = LogManager.getLogger(NoteServiceImplV2.class);

    private final NoteRepository noteRepository;
    @Autowired
    private org.springframework.data.mongodb.core.ReactiveMongoTemplate moduleTemplate;

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    private final FileUploadService fileUploadService;

    private final ObjectMapper objectMapper;

    private final ImageRecognitionClient imageRecognitionClient;

    private final RecognizedTextOrganizer recognizedTextOrganizer;

    private final PrivateCaptureImageService privateCaptureImageService;

    private final ConcurrentMap<String, Mono<NoteBaseResponse<Note>>> captureRequests =
            new ConcurrentHashMap<>();

    public NoteServiceImplV2(NoteRepository noteRepository, FileUploadService fileUploadService, ObjectMapper objectMapper) {
        this(noteRepository, fileUploadService, objectMapper, null, null, null);
    }

    public NoteServiceImplV2(NoteRepository noteRepository,
                             FileUploadService fileUploadService,
                             ObjectMapper objectMapper,
                             ImageRecognitionClient imageRecognitionClient) {
        this(noteRepository, fileUploadService, objectMapper, imageRecognitionClient, null, null);
    }

    public NoteServiceImplV2(NoteRepository noteRepository,
                             FileUploadService fileUploadService,
                             ObjectMapper objectMapper,
                             ImageRecognitionClient imageRecognitionClient,
                             RecognizedTextOrganizer recognizedTextOrganizer) {
        this(noteRepository, fileUploadService, objectMapper, imageRecognitionClient,
                recognizedTextOrganizer, null);
    }

    @Autowired
    public NoteServiceImplV2(NoteRepository noteRepository,
                             FileUploadService fileUploadService,
                             ObjectMapper objectMapper,
                             ImageRecognitionClient imageRecognitionClient,
                             RecognizedTextOrganizer recognizedTextOrganizer,
                             PrivateCaptureImageService privateCaptureImageService) {
        this.noteRepository = noteRepository;
        this.fileUploadService = fileUploadService;
        this.objectMapper = objectMapper;
        this.imageRecognitionClient = imageRecognitionClient;
        this.recognizedTextOrganizer = recognizedTextOrganizer;
        this.privateCaptureImageService = privateCaptureImageService;
        this.snowflakeIdGenerator = SnowflakeIdGenerator.getInstance();
    }

    private static NoteFilterV2 buildFilter(String deviceId, SearchNoteRequest request) {
        NoteFilterV2 noteFilterV2 = request.buildFilterV2(deviceId);
        if (StringUtils.isNotBlank(request.getKeyword())) {
            List<Term> termList = HanLP.segment(request.getKeyword());
            String str = termList.stream().map(term -> term.word).filter(StringUtils::isNotBlank).collect(Collectors.joining(" "));
            noteFilterV2.setKeyword(str);
        }
        return noteFilterV2;
    }

    @Override
    public Mono<NoteBaseResponse<Note>> createImageNote(String deviceId, FilePart filePart) {
        return createImageNote(deviceId, filePart, null, null);
    }

    @Override
    public Mono<NoteBaseResponse<Note>> createImageNote(
            String deviceId, FilePart filePart, String recognizedText) {
        return createImageNote(deviceId, filePart, recognizedText, null);
    }

    @Override
    public Mono<NoteBaseResponse<Note>> createImageNote(
            String deviceId, FilePart filePart, String recognizedText, String requestId) {
        logger.info("Creating image note");
        HttpHeaders headers = filePart.headers();
        String contentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
        if (contentType == null || !contentType.startsWith("image/")) {
            return Mono.just(NoteBaseResponse.<Note>failure("Only image file can be uploaded."));
        }
        String normalizedRecognizedText = org.apache.commons.lang3.StringUtils.trimToEmpty(recognizedText);
        if (normalizedRecognizedText.length() > 100_000) {
            return Mono.just(NoteBaseResponse.<Note>failure("Recognized text is too large."));
        }
        if (normalizedRecognizedText.isBlank() && imageRecognitionClient == null) {
            return Mono.just(NoteBaseResponse.<Note>failure("Image recognition is unavailable."));
        }
        if (!normalizedRecognizedText.isBlank() && recognizedTextOrganizer == null) {
            return Mono.just(NoteBaseResponse.<Note>failure("Image note organization is unavailable."));
        }
        if (privateCaptureImageService == null) {
            return Mono.just(NoteBaseResponse.<Note>failure("Private capture storage is unavailable."));
        }
        String normalizedRequestId = org.apache.commons.lang3.StringUtils.trimToEmpty(requestId);
        if (normalizedRequestId.isBlank()) normalizedRequestId = UUID.randomUUID().toString();
        if (!normalizedRequestId.matches("[A-Za-z0-9._:-]{1,100}")) {
            return Mono.just(NoteBaseResponse.<Note>failure("Capture request id is invalid."));
        }
        String stableRequestId = normalizedRequestId;
        String inFlightKey = deviceId + "\u0000" + stableRequestId;
        Mono<NoteBaseResponse<Note>> created = Mono.defer(() ->
                        noteRepository.findFirstByDeviceIdAndCaptureRequestId(deviceId, stableRequestId)
                                .map(NoteBaseResponse::success)
                                .switchIfEmpty(Mono.defer(() -> createPrivateImageNote(
                                        deviceId, filePart, normalizedRecognizedText, stableRequestId))))
                .doFinally(signal -> captureRequests.remove(inFlightKey))
                .cache();
        Mono<NoteBaseResponse<Note>> existing = captureRequests.putIfAbsent(inFlightKey, created);
        return existing == null ? created : existing;

    }

    private Mono<NoteBaseResponse<Note>> createPrivateImageNote(
            String deviceId, FilePart filePart, String recognizedText, String requestId) {
        return privateCaptureImageService.store(deviceId, filePart)
                .flatMap(stored -> {
                    Mono<ImageRecognitionClient.ImageRecognitionResult> recognitionResult =
                            recognizedText.isBlank()
                                    ? privateCaptureImageService.readOwnedBytes(deviceId, stored.id())
                                    .flatMap(bytes -> imageRecognitionClient.recognizeImageBytes(
                                            bytes, stored.mimeType()))
                                    : recognizedTextOrganizer.organize(recognizedText)
                                    .map(organized -> new ImageRecognitionClient.ImageRecognitionResult(
                                            recognizedText, organized.title(), organized.content()));
                    return recognitionResult.flatMap(recognized -> {
                                Note note = new Note();
                                note.setId(stableCaptureNoteId(deviceId, requestId));
                                note.setDeviceId(deviceId);
                                note.setImageUrl(stored.url());
                                note.setRawOcrText(recognized.recognizedText());
                                note.setCaptureRequestId(requestId);
                                note.setTitle(recognized.title());
                                TextDataModule textDataModule = new TextDataModule();
                                textDataModule.setModuleId(snowflakeIdGenerator.nextFullId(NoteModule.class));
                                textDataModule.setContent(recognized.organizedContent());
                                textDataModule.setSegmentation(SegmentUtil.segment(
                                        recognized.title(), recognized.organizedContent()));
                                note.setModules(List.of(textDataModule));
                                note.setDimension(0);
                                note.setNoteType(0);
                                note.setCreatedAt(LocalDateTime.now());
                                note.setUpdatedAt(LocalDateTime.now());
                                return noteRepository.insert(note)
                                        .map(NoteBaseResponse::success)
                                        .onErrorResume(DuplicateKeyException.class, duplicate ->
                                                privateCaptureImageService
                                                        .deleteOwned(deviceId, stored.id())
                                                        .then(noteRepository.findById(note.getId())
                                                                .filter(existing -> Objects.equals(
                                                                                existing.getDeviceId(), deviceId)
                                                                        && Objects.equals(
                                                                                existing.getCaptureRequestId(), requestId))
                                                                .map(NoteBaseResponse::success)
                                                                .switchIfEmpty(Mono.error(duplicate))));
                            })
                            .onErrorResume(failure -> privateCaptureImageService
                                    .deleteOwned(deviceId, stored.id())
                                    .then(Mono.error(failure)));
                })
                .onErrorResume(error -> {
                    logger.warn("Private image note creation failed");
                    return Mono.just(NoteBaseResponse.<Note>failure(
                            "Unable to recognize readable text in this image."));
                });
    }

    private String stableCaptureNoteId(String ownerId, String requestId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((ownerId + "\u0000" + requestId).getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder("capture-");
            for (byte item : digest) value.append(String.format("%02x", item & 0xff));
            return value.toString();
        } catch (Exception impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    @Override
    public Mono<NoteBaseResponse<Note>> createNote(String deviceId, CreateNoteRequest request) {
        logger.info("Creating note");
        if (StringUtils.isBlank(request.getContent()) && StringUtils.isBlank(request.getTitle())) {
            return Mono.just(NoteBaseResponse.failure("备忘录的内容和标题不能同时为空"));
        }
        Note note = new Note();
        note.setId(snowflakeIdGenerator.nextFullId(Note.class));
        note.setDeviceId(deviceId);
        note.setTitle(StringUtils.isEmpty(request.getTitle()) ? "" : request.getTitle());
        if (StringUtils.isNotBlank(request.getContent())) {
            TextDataModule textDataModule = new TextDataModule();
            textDataModule.setModuleId(snowflakeIdGenerator.nextFullId(NoteModule.class));
            textDataModule.setContent(request.getContent());
            textDataModule.setSegmentation(SegmentUtil.segment(request.getTitle(), request.getContent()));
            note.setModules(List.of(textDataModule));
        }
        note.setDimension(request.getDimension());
        note.setNoteType(request.getNoteType() == null ? 0 : request.getNoteType());
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        Stream<NoteModule> newModuleStream = Optional.ofNullable(request.getModuleList()).orElse(List.of()).stream();
        note.setModules(Stream.concat(note.getModules() == null ? Stream.of() : note.getModules().stream(), newModuleStream).toList());
        return noteRepository.save(note).map(NoteBaseResponse::success);
        //.flatMap(inserted -> noteRepository.findById(inserted.getId()));
    }

    @Override
    public Mono<Note> getNoteById(String deviceId, String id) {
        return noteRepository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(
                        "NOTE_NOT_FOUND", "Note was not found")))
                .flatMap(note -> Objects.equals(note.getDeviceId(), deviceId)
                        ? Mono.just(note)
                        : Mono.error(new BusinessException(
                                "NOTE_FORBIDDEN",
                                "The authenticated user does not own this note")));
    }

    @Override
    public Mono<Note> findNoteById(String id) {
        return noteRepository.findById(id);
    }


    @Override
    public Flux<Note> listNote(String deviceId, SearchNoteRequest request) {
        logger.info("Listing notes");
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize(), request.isAscending() ? Sort.by(request.getSortBy()).ascending() : Sort.by(request.getSortBy()).descending());
        NoteFilterV2 noteFilterV2 = buildFilter(deviceId, request);
        return noteRepository.findByDynamicCriteria(noteFilterV2, pageable);
    }

    @Override
    public Flux<Note> listRelatedNoteCandidates(String ownerId, String excludedNoteId) {
        return noteRepository.findByDeviceIdAndDeletedFalseAndDimension(ownerId, 0)
                .filter(note -> java.util.Objects.equals(ownerId, note.getDeviceId()))
                .filter(note -> !note.isDeleted() && note.getDimension() == 0)
                .filter(note -> !java.util.Objects.equals(note.getId(), excludedNoteId))
                .filter(note -> StringUtils.isNotBlank(note.getTitle())
                        || StringUtils.isNotBlank(note.getContent()));
    }

    @Override
    public Mono<NoteBaseResponse<NotePageData<Note>>> notePagination(String deviceId, SearchNoteRequest request) {
        logger.info("Listing notes with pagination");
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize(), request.isAscending() ? Sort.by(request.getSortBy()).ascending() : Sort.by(request.getSortBy()).descending());

        // 将总数、分页数据和元数据打包成自定义的 PageData 对象
        return count(deviceId, request)
                .flatMap(total -> {
                    if (total == 0L) {
                        return Mono.just(NoteBaseResponse.<NotePageData<Note>>success());
                    }
                    long totalPages = (total + request.getSize() - 1) / request.getSize();  // 计算总页数
                    NoteFilterV2 noteFilterV2 = buildFilter(deviceId, request);
                    return noteRepository.findByDynamicCriteria(noteFilterV2, pageable)
                            .collectList()
                            .map(list -> NoteBaseResponse.success(new NotePageData<>(list, request.getPage(), totalPages, total)));
                }).onErrorResume((error) -> {
                    logger.error("Failed to list notes");
                    return Mono.just(NoteBaseResponse.failure("无法获取备忘录列表, 错误原因是: " + error.getMessage()));
                });
    }

    @Override
    public Mono<NoteBaseResponse<Note>> updateNote(String deviceId, UpdateNoteRequest request) {
        return noteRepository.findById(request.getId())
                .flatMap(exists -> {
                    if (!exists.getDeviceId().equals(deviceId)) {
                        logger.error("Note update denied because ownership did not match");
                        return Mono.just(NoteBaseResponse.failure("不能更新别人创建的备忘录"));
                    }
                    if (request.getTitle() == null
                            && request.getNoteType() == null) {
                        return Mono.just(NoteBaseResponse.failure("备忘录的标题和类型不能同时为空"));
                    }
                    if (request.getTitle() != null) {
                        exists.setTitle(request.getTitle());
                    }
                    if (request.getNoteType() != null) {
                        exists.setNoteType(request.getNoteType());
                    }
                    if (request.getThemeId() != null) {
                        exists.setNoteThemeIds(Stream.concat(exists.getNoteThemeIds().stream(), Optional.of(request.getThemeId()).stream()).toList());
                    }
                    exists.setUpdatedAt(LocalDateTime.now());
                    return noteRepository.save(exists).map(NoteBaseResponse::success);
                });
    }


    @Override
    public Mono<List<Note>> addNotesThemeId(List<String> noteIds, String themeId) {
        // 使用 findByIds 方法查询所有笔记
        return noteRepository.findAllById(noteIds)
                .collectList()
                .flatMap(notes -> {
                    List<Note> updatedNotes = notes.stream().peek(note -> {
                        note.setNoteThemeIds(Stream.concat(note.getNoteThemeIds().stream(), Optional.ofNullable(themeId).stream()).toList());
                        note.setUpdatedAt(LocalDateTime.now());
                    }).toList();
                    return noteRepository.saveAll(updatedNotes)
                            .collectList();
                });
    }

    @Override
    public Mono<NoteBaseResponse<Note>> deleteNote(String deviceId, String id) {
        return noteRepository.findById(id)
                .flatMap(exists -> {
                    if (!exists.getDeviceId().equals(deviceId)) {
                        logger.error("Note deletion denied because ownership did not match");
                        return Mono.just(NoteBaseResponse.failure("不能删除别人创建的备忘录"));
                    }
                    exists.setDeleted(true);
                    exists.setDeletedAt(LocalDateTime.now());
                    return noteRepository.save(exists).map(NoteBaseResponse::success);
                });
    }

    @Override
    public Mono<Long> count(String deviceId, SearchNoteRequest request) {
        NoteFilterV2 noteFilterV2 = buildFilter(deviceId, request);
        return noteRepository.countByDynamicCriteria(noteFilterV2);
    }

    @Override
    public Flux<Note> getNotesByIds(Set<String> ids) {
        return noteRepository.findAllById(ids);
    }

    @Override
    public Flux<Note> getNotesByIds(List<String> ids) {
        return noteRepository.findAllById(ids);
    }

    @Override
    public Mono<NoteBaseResponse<Note>> addNoteModule(String deviceId, AddNoteModuleRequest request) {
        return noteRepository.findById(request.getId())
                .<NoteBaseResponse<Note>>flatMap(exists -> {
                    if (!exists.getDeviceId().equals(deviceId)) {
                        logger.error("Note module change denied because ownership did not match");
                        return Mono.just(NoteBaseResponse.failure("不能在别人创建的备忘录上添加模块"));
                    }
                    NoteModule noteModule;
                    if (exists.isDeleted()) return Mono.just(NoteBaseResponse.failure("备忘录已删除"));
                    // Atomic append, never write a stale copy of the note's text/modules.
                    // A deterministic module id also makes retries after lost responses safe.
                    try {
                        byte[] payload = objectMapper.writeValueAsBytes(request.getModule());
                        String hash = java.util.HexFormat.of().formatHex(
                                java.security.MessageDigest.getInstance("SHA-256").digest(payload));
                        noteModule = buildNoteModule(request, "saved-module-" + hash);
                    } catch (Exception failure) {
                        return Mono.error(new IllegalStateException("Cannot prepare module save"));
                    }
                    var query = org.springframework.data.mongodb.core.query.Query.query(
                            org.springframework.data.mongodb.core.query.Criteria.where("_id").is(exists.getId())
                                    .and("deviceId").is(deviceId).and("deleted").ne(true)
                                    .and("modules.moduleId").ne(noteModule.getModuleId()));
                    var update = new org.springframework.data.mongodb.core.query.Update()
                            .push("modules", noteModule).set("updatedAt", LocalDateTime.now());
                    return moduleTemplate.findAndModify(query, update,
                                    org.springframework.data.mongodb.core.FindAndModifyOptions.options().returnNew(true), Note.class)
                            .switchIfEmpty(noteRepository.findById(exists.getId())
                                    .filter(note -> deviceId.equals(note.getDeviceId()) && !note.isDeleted())
                                    .switchIfEmpty(Mono.error(new IllegalStateException("Note no longer available"))))
                            .map(NoteBaseResponse::success);
                }).onErrorResume((error) -> {
                    logger.error("Failed to add note module");
                    return Mono.just(NoteBaseResponse.failure("无法添加模块, 错误原因是: " + error.getMessage()));
                });
    }

    @Override
    public Mono<NoteBaseResponse<Note>> updateNoteModule(String deviceId, UpdateNoteModuleRequest request) {
        return noteRepository.findById(request.getId())
                .<NoteBaseResponse<Note>>flatMap(exists -> {
                    if (!exists.getDeviceId().equals(deviceId)) {
                        logger.error("Note module change denied because ownership did not match");
                        return Mono.just(NoteBaseResponse.failure("不能在别人创建的备忘录上更新"));
                    }
                    if (exists.isDeleted()) return Mono.just(NoteBaseResponse.failure("备忘录已删除"));
                    int index = -1;
                    for (int i = 0; i < exists.getModules().size(); i++) {
                        if (java.util.Objects.equals(request.getModuleId(), exists.getModules().get(i).getModuleId())) {
                            index = i;
                            break;
                        }
                    }
                    if (index < 0) return Mono.just(NoteBaseResponse.failure("笔记模块不存在"));
                    NoteModule module = exists.getModules().get(index);
                    String path = "modules." + index;
                    var criteria = org.springframework.data.mongodb.core.query.Criteria.where("_id").is(exists.getId())
                            .and("deviceId").is(deviceId).and("deleted").ne(true)
                            .and(path + ".moduleId").is(request.getModuleId());
                    if (module instanceof TextDataModule text) {
                        criteria.and(path + ".content").is(text.getContent());
                    } else {
                        criteria.and("updatedAt").is(exists.getUpdatedAt());
                    }
                    replenishNoteModule(module, request);
                    // Never overwrite other modules or voice-summary idempotency metadata.
                    var update = new org.springframework.data.mongodb.core.query.Update()
                            .set(path, module).set("updatedAt", LocalDateTime.now());
                    return moduleTemplate.findAndModify(org.springframework.data.mongodb.core.query.Query.query(criteria),
                                    update, org.springframework.data.mongodb.core.FindAndModifyOptions.options().returnNew(true), Note.class)
                            .map(NoteBaseResponse::success)
                            .switchIfEmpty(Mono.just(NoteBaseResponse.failure("备忘录已被更新，请刷新后重试；未覆盖其他内容")));
                }).onErrorResume((error) -> {
                    logger.error("Failed to update note module");
                    return Mono.just(NoteBaseResponse.failure("无法更新当前模块, 错误原因是: " + error.getMessage()));
                });
    }

    @Override
    public Mono<NoteBaseResponse<Note>> deleteNoteModule(String deviceId, DeleteNoteModuleRequest request) {
        return noteRepository.findById(request.getId())
                .<NoteBaseResponse<Note>>flatMap(exists -> {
                    if (!exists.getDeviceId().equals(deviceId)) {
                        logger.error("Note module change denied because ownership did not match");
                        return Mono.just(NoteBaseResponse.failure("不能在别人创建的备忘录上更新"));
                    }
                    exists.setModules(exists.getModules().stream()
                            .filter(module -> !module.getModuleId().equals(request.getModuleId())).toList());
                    exists.setUpdatedAt(LocalDateTime.now());
                    return noteRepository.save(exists).map(NoteBaseResponse::success);
                }).onErrorResume((error) -> {
                    logger.error("Failed to update note module");
                    return Mono.just(NoteBaseResponse.failure("无法删除当前模块, 错误原因是: " + error.getMessage()));
                });
    }

    @Override
    public Mono<NoteBaseResponse<Note>> moveNoteModule(String deviceId, MoveNoteModuleRequest request) {
        return noteRepository.findById(request.getId())
                .<NoteBaseResponse<Note>>flatMap(exists -> {
                    if (!exists.getDeviceId().equals(deviceId)) {
                        logger.error("Note module change denied because ownership did not match");
                        return Mono.just(NoteBaseResponse.failure("不能在别人创建的备忘录上更新"));
                    }
                    NoteModule targetModule = exists.getModules().stream().filter(module -> module.getModuleId().equals(request.getModuleId())).findFirst().orElse(null);
                    if (targetModule == null) {
                        return Mono.just(NoteBaseResponse.failure("目标模块不存在"));
                    }
                    if (request.getModuleId() == null) {
                        exists.setModules(Stream.concat(Stream.of(targetModule), exists.getModules().stream().filter(module -> !module.getModuleId().equals(request.getModuleId()))).toList());
                    } else {
                        exists.setModules(
                                exists.getModules().stream()
                                        .flatMap(module -> {
                                            if (module.getModuleId().equals(request.getModuleId())) {
                                                return Stream.of();
                                            } else if (module.getModuleId().equals(request.getMoveAfter())) {
                                                return Stream.of(module, targetModule);
                                            } else {
                                                return Stream.of(module);
                                            }
                                        }).toList());
                    }
                    exists.setUpdatedAt(LocalDateTime.now());
                    return noteRepository.save(exists).map(NoteBaseResponse::success);
                }).onErrorResume((error) -> {
                    logger.error("Failed to update note module");
                    return Mono.just(NoteBaseResponse.failure("无法删除当前模块, 错误原因是: " + error.getMessage()));
                });
    }

    @Override
    public Mono<NoteBaseResponse<Note>> addNoteModuleItem(String deviceId, AddNoteModuleItemRequest request) {
        return noteRepository.findById(request.getId())
                .<NoteBaseResponse<Note>>flatMap(exists -> {
                    if (!exists.getDeviceId().equals(deviceId)) {
                        logger.error("Note module change denied because ownership did not match");
                        return Mono.just(NoteBaseResponse.failure("不能在别人创建的备忘录上添加模块"));
                    }
                    NoteModule targetModule = exists.getModules().stream().filter(module -> module.getModuleId().equals(request.getModuleId())).findFirst().orElse(null);
                    if (targetModule == null) {
                        return Mono.just(NoteBaseResponse.failure("目标模块不存在"));
                    }
                    if (targetModule.getNoteModuleType() != request.getItem().getNoteModuleType()) {
                        return Mono.just(NoteBaseResponse.failure("目标模块类型不匹配"));
                    }
                    appendNoteModuleItem(targetModule, request.getItem());
                    exists.setUpdatedAt(LocalDateTime.now());
                    return noteRepository.save(exists).map(NoteBaseResponse::success);
                }).onErrorResume((error) -> {
                    logger.error("Failed to add note module");
                    return Mono.just(NoteBaseResponse.failure("无法添加模块, 错误原因是: " + error.getMessage()));
                });
    }

    @Override
    public Mono<NoteBaseResponse<Note>> updateNoteModuleItem(String deviceId, UpdateNoteModuleItemRequest request) {
        return noteRepository.findById(request.getId())
                .<NoteBaseResponse<Note>>flatMap(exists -> {
                    if (!exists.getDeviceId().equals(deviceId)) {
                        logger.error("Note module change denied because ownership did not match");
                        return Mono.just(NoteBaseResponse.failure("不能在别人创建的备忘录上添加模块"));
                    }
                    NoteModule targetModule = exists.getModules().stream().filter(module -> module.getModuleId().equals(request.getModuleId())).findFirst().orElse(null);
                    if (targetModule == null) {
                        return Mono.just(NoteBaseResponse.failure("目标模块不存在"));
                    }
                    if (targetModule.getNoteModuleType() != request.getItem().getNoteModuleType()) {
                        return Mono.just(NoteBaseResponse.failure("目标模块类型不匹配"));
                    }
                    updateNoteModuleItemDetail(targetModule, request);
                    exists.setUpdatedAt(LocalDateTime.now());
                    return noteRepository.save(exists).map(NoteBaseResponse::success);
                }).onErrorResume((error) -> {
                    logger.error("Failed to add note module");
                    return Mono.just(NoteBaseResponse.failure("无法添加模块, 错误原因是: " + error.getMessage()));
                });
    }

    @Override
    public Mono<NoteBaseResponse<Note>> deleteNoteModuleItem(String deviceId, DeleteNoteModuleItemRequest request) {
        return noteRepository.findById(request.getId())
                .<NoteBaseResponse<Note>>flatMap(exists -> {
                    if (!exists.getDeviceId().equals(deviceId)) {
                        logger.error("Note module change denied because ownership did not match");
                        return Mono.just(NoteBaseResponse.failure("不能在别人创建的备忘录上更新"));
                    }
                    NoteModule targetModule = exists.getModules().stream().filter(module -> module.getModuleId().equals(request.getModuleId())).findFirst().orElse(null);
                    if (targetModule == null) {
                        return Mono.just(NoteBaseResponse.failure("目标模块不存在"));
                    }
                    removeNoteModuleItem(targetModule, request.getItemId());
                    exists.setUpdatedAt(LocalDateTime.now());
                    return noteRepository.save(exists).map(NoteBaseResponse::success);
                }).onErrorResume((error) -> {
                    logger.error("Failed to update note module");
                    return Mono.just(NoteBaseResponse.failure("无法删除当前模块, 错误原因是: " + error.getMessage()));
                });
    }

    @Override
    public Mono<NoteBaseResponse<Note>> replaceTextModule(String deviceId, ReplaceTextModuleRequest request) {
        return noteRepository.findById(request.getId())
                .<NoteBaseResponse<Note>>flatMap(exists -> {
                    if (!exists.getDeviceId().equals(deviceId)) {
                        logger.error("Note module change denied because ownership did not match");
                        return Mono.just(NoteBaseResponse.failure("不能在别人创建的备忘录上更新"));
                    }
                    NoteReplaceTextModuleLog replaceTextModuleLog = new NoteReplaceTextModuleLog();
                    TextDataModule targetModule = exists.getModules().stream().filter(module -> module instanceof TextDataModule).map(TextDataModule.class::cast).findFirst().orElse(null);
                    if (targetModule == null) {
                        return Mono.just(NoteBaseResponse.failure("目标模块不存在"));
                    }
                    replaceTextModuleLog.setOldModule(SerializationUtils.clone(targetModule));
                    targetModule.setContent(request.getOrganizedNote());
                    targetModule.setTitle(request.getTitle());
                    targetModule.setSegmentation(SegmentUtil.segment(request.getTitle(), targetModule.getCleanedContent()));
                    targetModule.setItems(request.getNoteModuleItems().stream().map(itemRequest -> {
                        if (itemRequest instanceof ReplaceTextModuleRequest.RecommendationItemRequest recommendationItemRequest) {
                            RecommendationItem recommendationItem = new RecommendationItem();
                            recommendationItem.setItemId(snowflakeIdGenerator.nextFullId(NoteModuleItem.class));
                            recommendationItem.setSuggestion(recommendationItemRequest.getSuggestion());
                            recommendationItem.setEmoji(recommendationItemRequest.getEmoji());
                            recommendationItem.setRecommendType(RecommendationItemType.SUGGESTION);
                            return recommendationItem;
                        } else if (itemRequest instanceof ReplaceTextModuleRequest.BacklogItemRequest backlogItemRequest) {
                            BacklogItem backlogItem = new BacklogItem();
                            backlogItem.setItemId(snowflakeIdGenerator.nextFullId(NoteModuleItem.class));
                            backlogItem.setScheduledAt(backlogItemRequest.getScheduledAt());
                            backlogItem.setContent(backlogItemRequest.getContent());
                            backlogItem.setDescription(backlogItemRequest.getDescription());
                            backlogItem.setNeedNotify(backlogItemRequest.isNeedNotify());
                            backlogItem.setSegmentation(SegmentUtil.segment(backlogItemRequest.getContent(), backlogItemRequest.getDescription()));
                            return backlogItem;
                        } else {
                            throw new IllegalStateException("Unexpected note item type: " + itemRequest.getNoteItemType());
                        }
                    }).toList());
                    replaceTextModuleLog.setNewModule(targetModule);
                    replaceTextModuleLog.setOccurredAt(LocalDateTime.now());
                    exists.setOperationLogs(exists.getOperationLogs() == null ? List.of(replaceTextModuleLog) : Stream.concat(exists.getOperationLogs().stream(), Stream.of(replaceTextModuleLog)).toList());
                    exists.setUpdatedAt(LocalDateTime.now());
                    return noteRepository.save(exists).map(NoteBaseResponse::success);
                }).onErrorResume((error) -> {
                    logger.error("Failed to update note module");
                    return Mono.just(NoteBaseResponse.failure("无法删除当前模块, 错误原因是: " + error.getMessage()));
                });
    }

    private void replenishNoteModule(NoteModule module, UpdateNoteModuleRequest request) {
        switch (module) {
            case AIPictureModule aiPictureModule -> {
                UpdateNoteModuleRequest.UpdateAIPictureModuleRequest updateAIPictureModuleRequest = (UpdateNoteModuleRequest.UpdateAIPictureModuleRequest) request.getModule();
                Optional.ofNullable(updateAIPictureModuleRequest.getImageUrl()).ifPresent(aiPictureModule::setImageUrl);
                Optional.ofNullable(updateAIPictureModuleRequest.getDescription()).ifPresent(aiPictureModule::setDescription);
                Optional.ofNullable(updateAIPictureModuleRequest.getTitle()).ifPresent(aiPictureModule::setTitle);
                aiPictureModule.setSegmentation(SegmentUtil.segment(aiPictureModule.getTitle(), aiPictureModule.getDescription()));
            }
            case AIRecommendationModule aiRecommendationModule -> {
                UpdateNoteModuleRequest.UpdateAIRecommendationModuleRequest updateAIRecommendationModuleRequest = (UpdateNoteModuleRequest.UpdateAIRecommendationModuleRequest) request.getModule();
                Optional.ofNullable(updateAIRecommendationModuleRequest.getTitle()).ifPresent(aiRecommendationModule::setTitle);
                Optional.ofNullable(updateAIRecommendationModuleRequest.getTitle()).ifPresent(ignored -> aiRecommendationModule.setSegmentation(SegmentUtil.segment(aiRecommendationModule.getTitle())));
            }
            case BacklogModule backlogModule -> {
                UpdateNoteModuleRequest.UpdateBacklogModuleRequest updateBacklogModuleRequest = (UpdateNoteModuleRequest.UpdateBacklogModuleRequest) request.getModule();
                Optional.ofNullable(updateBacklogModuleRequest.getName()).ifPresent(backlogModule::setName);
                Optional.ofNullable(updateBacklogModuleRequest.getName()).ifPresent(ignored -> backlogModule.setSegmentation(SegmentUtil.segment(backlogModule.getName())));
            }
            case QAModule ignored -> {
            }
            case KeyValueModule ignored -> {
            }
            case RelatedNoteModule relatedNoteModule -> {
                UpdateNoteModuleRequest.UpdateRelatedNoteModuleRequest updateBacklogModuleRequest = (UpdateNoteModuleRequest.UpdateRelatedNoteModuleRequest) request.getModule();
                relatedNoteModule.setRelatedNoteIds(updateBacklogModuleRequest.getRelatedNoteIds());
            }
            case TextDataModule textDataModule -> {
                UpdateNoteModuleRequest.UpdateTextDataModuleRequest updateTextDataModuleRequest = (UpdateNoteModuleRequest.UpdateTextDataModuleRequest) request.getModule();
                textDataModule.setTitle(updateTextDataModuleRequest.getTitle());
                textDataModule.setContent(updateTextDataModuleRequest.getContent());
                textDataModule.setSegmentation(SegmentUtil.segment(updateTextDataModuleRequest.getTitle(), updateTextDataModuleRequest.getContent()));
            }
            case ScenarioRecordModule sceneConversationModule -> {
                UpdateNoteModuleRequest.UpdateScenarioRecordModuleRequest updateScenarioRecordModuleRequest = (UpdateNoteModuleRequest.UpdateScenarioRecordModuleRequest) request.getModule();
                sceneConversationModule.setContent(updateScenarioRecordModuleRequest.getContent());
                sceneConversationModule.setSegmentation(SegmentUtil.segment(updateScenarioRecordModuleRequest.getContent()));
            }
            default -> throw new IllegalStateException("Unexpected value: " + module.getNoteModuleType());
        }
    }

    @Nullable
    private NoteModule buildNoteModule(AddNoteModuleRequest request, String moduleId) {
        AddNoteModuleRequest.CreateNoteModulePayload createNoteModulePayload = request.getModule();
        return switch (createNoteModulePayload) {
            case AddNoteModuleRequest.CreateAIPictureModuleRequest aiPictureModuleRequest -> {
                AIPictureModule aiPictureModule = new AIPictureModule();
                aiPictureModule.setModuleId(moduleId);
                aiPictureModule.setTitle(aiPictureModuleRequest.getTitle());
                aiPictureModule.setImageUrl(aiPictureModuleRequest.getImageUrl());
                aiPictureModule.setDescription(aiPictureModuleRequest.getDescription());
                aiPictureModule.setSegmentation(SegmentUtil.segment(aiPictureModule.getDescription()));
                yield aiPictureModule;
            }
            case AddNoteModuleRequest.CreateAIRecommendationModuleRequest aiRecommendationModuleRequest -> {
                AIRecommendationModule aiRecommendationModule = new AIRecommendationModule();
                aiRecommendationModule.setModuleId(moduleId);
                aiRecommendationModule.setTitle(aiRecommendationModuleRequest.getTitle());
                aiRecommendationModule.setSegmentation(SegmentUtil.segment(aiRecommendationModuleRequest.getTitle()));
                aiRecommendationModule.setItems(aiRecommendationModuleRequest.getRecommendationItems().stream().map(recommendationItemRequest -> {
                    RecommendationItem item = new RecommendationItem();
                    item.setItemId(snowflakeIdGenerator.nextFullId(NoteModuleItem.class));
                    item.setSuggestion(recommendationItemRequest.getSuggestion());
                    item.setEmoji(recommendationItemRequest.getEmoji());
                    item.setProductKeyword(recommendationItemRequest.getProductKeyword());
                    item.setRecommendType(recommendationItemRequest.getRecommendType());
                    item.setProductRecommendations(recommendationItemRequest.getProductRecommendations());
                    String productRecommendationStr = Optional.ofNullable(recommendationItemRequest.getProductRecommendations())
                            .map(productRecommendationItems -> productRecommendationItems
                                    .stream()
                                    .flatMap(productRecommendationItem -> Stream.of(
                                            productRecommendationItem.getProductDesc(),
                                            productRecommendationItem.getProductName(),
                                            productRecommendationItem.getRecommendationReason()))
                                    .collect(Collectors.joining("\n")))
                            .orElse("");
                    item.setSegmentation(SegmentUtil.segment(recommendationItemRequest.getSuggestion(), recommendationItemRequest.getProductKeyword(), productRecommendationStr));
                    return item;
                }).toList());
                yield aiRecommendationModule;
            }
            case AddNoteModuleRequest.CreateQAModuleRequest qaModuleRequest -> {
                QAModule qaModule = new QAModule();
                qaModule.setModuleId(moduleId);
                qaModule.setItems(qaModuleRequest.getQuestionAnswerItems().stream().map(questionAnswerItemRequest -> {
                    QAItem item = new QAItem();
                    item.setItemId(snowflakeIdGenerator.nextFullId(NoteModuleItem.class));
                    item.setQuestion(questionAnswerItemRequest.getQuestion());
                    item.setAnswer(questionAnswerItemRequest.getAnswer());
                    item.setSegmentation(SegmentUtil.segment(questionAnswerItemRequest.getQuestion(), questionAnswerItemRequest.getAnswer()));
                    return item;
                }).toList());
                yield qaModule;
            }
            case AddNoteModuleRequest.CreateBacklogModuleRequest backlogModuleRequest -> {
                BacklogModule backlogModule = new BacklogModule();
                backlogModule.setModuleId(moduleId);
                backlogModule.setName(backlogModuleRequest.getName());
                backlogModule.setSegmentation(SegmentUtil.segment(backlogModuleRequest.getName()));
                backlogModule.setItems(backlogModuleRequest.getBacklogItems().stream().map(itemRequest -> {
                    BacklogItem item = new BacklogItem();
                    item.setItemId(snowflakeIdGenerator.nextFullId(NoteModuleItem.class));
                    item.setScheduledAt(itemRequest.getScheduledAt());
                    item.setContent(itemRequest.getContent());
                    item.setDescription(itemRequest.getDescription());
                    item.setNeedNotify(itemRequest.isNeedNotify());
                    item.setSegmentation(SegmentUtil.segment(itemRequest.getContent(), itemRequest.getDescription()));
                    return item;
                }).toList());
                yield backlogModule;
            }
            case AddNoteModuleRequest.CreateRelatedNoteModuleRequest relatedNoteModuleRequest -> {
                RelatedNoteModule relatedNoteModule = new RelatedNoteModule();
                relatedNoteModule.setModuleId(moduleId);
                relatedNoteModule.setRelatedNoteIds(relatedNoteModuleRequest.getRelatedNoteIds());
                yield relatedNoteModule;
            }
            case AddNoteModuleRequest.CreateTextDataModuleRequest textDataModuleRequest -> {
                TextDataModule textDataModule = new TextDataModule();
                textDataModule.setModuleId(moduleId);
                textDataModule.setTitle(textDataModuleRequest.getTitle());
                textDataModule.setContent(textDataModuleRequest.getContent());
                textDataModule.setSegmentation(SegmentUtil.segment(textDataModuleRequest.getTitle(), textDataModuleRequest.getContent()));
                yield textDataModule;
            }
            case AddNoteModuleRequest.CreateScenarioRecordModuleRequest sceneConversationModuleRequest -> {
                ScenarioRecordModule sceneConversationModule = new ScenarioRecordModule();
                sceneConversationModule.setModuleId(moduleId);
                sceneConversationModule.setContent(sceneConversationModuleRequest.getContent());
                sceneConversationModule.setSegmentation(SegmentUtil.segment(sceneConversationModule.getContent()));
                yield sceneConversationModule;
            }
            case AddNoteModuleRequest.CreateKeyValueModuleRequest keyValueModuleRequest -> {
                KeyValueModule textDataModule = new KeyValueModule();
                textDataModule.setModuleId(moduleId);
                textDataModule.setItems(keyValueModuleRequest.getKeyValueItems().stream().map(itemRequest -> {
                    KeyValueItem item = new KeyValueItem();
                    item.setItemId(snowflakeIdGenerator.nextFullId(NoteModuleItem.class));
                    item.setKey(itemRequest.getKey());
                    item.setValue(itemRequest.getValue());
                    item.setSegmentation(SegmentUtil.segment(itemRequest.getKey(), itemRequest.getValue()));
                    return item;
                }).toList());
                yield textDataModule;
            }
            default -> throw new IllegalStateException("Unexpected value: " + createNoteModulePayload);
        };
    }

    private void appendNoteModuleItem(NoteModule module, AddNoteModuleItemRequest.CreateNoteModuleItemPayload createNoteModuleItemPayload) {
        String itemId = snowflakeIdGenerator.nextFullId(NoteModuleItem.class);
        switch (module) {
            case AIRecommendationModule aiRecommendationModule -> {
                AddNoteModuleItemRequest.CreateAIRecommendationItemRequest createAIRecommendationItemRequest = (AddNoteModuleItemRequest.CreateAIRecommendationItemRequest) createNoteModuleItemPayload;
                RecommendationItem recommendationItem = new RecommendationItem();
                recommendationItem.setItemId(itemId);
                recommendationItem.setSuggestion(createAIRecommendationItemRequest.getDescription());
                recommendationItem.setSegmentation(SegmentUtil.segment(createAIRecommendationItemRequest.getDescription()));
                recommendationItem.setRecommendType(RecommendationItemType.SUGGESTION);
                aiRecommendationModule.getItems().add(recommendationItem);
            }
            case BacklogModule backlogModule -> {
                AddNoteModuleItemRequest.CreateBacklogItemRequest createBacklogItemRequest = (AddNoteModuleItemRequest.CreateBacklogItemRequest) createNoteModuleItemPayload;
                BacklogItem backlogItem = new BacklogItem();
                backlogItem.setItemId(itemId);
                backlogItem.setContent(createBacklogItemRequest.getContent());
                backlogItem.setDescription(createBacklogItemRequest.getDescription());
                backlogItem.setScheduledAt(createBacklogItemRequest.getScheduledAt());
                backlogItem.setNeedNotify(createBacklogItemRequest.isNeedNotify());
                backlogItem.setSegmentation(SegmentUtil.segment(createBacklogItemRequest.getContent(), createBacklogItemRequest.getDescription()));
                backlogModule.getItems().add(backlogItem);
            }
            case QAModule qaModule -> {
                AddNoteModuleItemRequest.CreateQAItemRequest createQAItemRequest = (AddNoteModuleItemRequest.CreateQAItemRequest) createNoteModuleItemPayload;
                QAItem qaItem = new QAItem();
                qaItem.setItemId(itemId);
                qaItem.setQuestion(createQAItemRequest.getQuestion());
                qaItem.setAnswer(createQAItemRequest.getAnswer());
                qaItem.setSegmentation(SegmentUtil.segment(createQAItemRequest.getQuestion(), createQAItemRequest.getAnswer()));
                qaModule.getItems().add(qaItem);
            }
            case KeyValueModule keyValueModule -> {
                AddNoteModuleItemRequest.CreateKeyValueItemRequest createQAItemRequest = (AddNoteModuleItemRequest.CreateKeyValueItemRequest) createNoteModuleItemPayload;
                KeyValueItem keyValueItem = new KeyValueItem();
                keyValueItem.setItemId(itemId);
                keyValueItem.setKey(createQAItemRequest.getKey());
                keyValueItem.setValue(createQAItemRequest.getValue());
                keyValueItem.setSegmentation(SegmentUtil.segment(createQAItemRequest.getKey(), createQAItemRequest.getValue()));
                keyValueModule.getItems().add(keyValueItem);
            }
            default -> throw new IllegalStateException("Unexpected value: " + module.getNoteModuleType());

        }
    }

    private void updateNoteModuleItemDetail(NoteModule module, UpdateNoteModuleItemRequest request) {
        UpdateNoteModuleItemRequest.UpdateNoteModuleItemPayload updateNoteModuleItemPayload = request.getItem();
        switch (module) {
            case AIRecommendationModule aiRecommendationModule -> {
                UpdateNoteModuleItemRequest.UpdateAIRecommendationItemRequest updateAIRecommendationItemRequest = (UpdateNoteModuleItemRequest.UpdateAIRecommendationItemRequest) updateNoteModuleItemPayload;
                aiRecommendationModule.getItems().forEach(item1 -> {
                    if (item1.getItemId().equals(request.getItemId())) {
                        item1.setSuggestion(updateAIRecommendationItemRequest.getDescription());
                        item1.setSegmentation(SegmentUtil.segment(updateAIRecommendationItemRequest.getDescription()));
                    }
                });
            }
            case BacklogModule backlogModule -> {
                UpdateNoteModuleItemRequest.UpdateBacklogItemRequest updateBacklogItemRequest = (UpdateNoteModuleItemRequest.UpdateBacklogItemRequest) updateNoteModuleItemPayload;
                backlogModule.getItems().forEach(item1 -> {
                    if (item1.getItemId().equals(request.getItemId())) {
                        Optional.ofNullable(updateBacklogItemRequest.getIsNeedNotify()).ifPresent(item1::setNeedNotify);
                        Optional.ofNullable(updateBacklogItemRequest.getContent()).ifPresent(item1::setContent);
                        Optional.ofNullable(updateBacklogItemRequest.getDescription()).ifPresent(item1::setDescription);
                        Optional.ofNullable(updateBacklogItemRequest.getScheduledAt()).ifPresent(item1::setScheduledAt);
                        Optional.ofNullable(updateBacklogItemRequest.getIsNeedNotify()).ifPresent(item1::setNeedNotify);
                        if (StringUtils.isNotBlank(item1.getContent()) || StringUtils.isNotBlank(item1.getDescription())) {
                            item1.setSegmentation(SegmentUtil.segment(item1.getContent(), item1.getDescription()));
                        }
                    }
                });
            }
            case QAModule qaModule -> {
                UpdateNoteModuleItemRequest.UpdateQAItemRequest updateQAItemRequest = (UpdateNoteModuleItemRequest.UpdateQAItemRequest) updateNoteModuleItemPayload;
                qaModule.getItems().forEach(item1 -> {
                    if (item1.getItemId().equals(request.getItemId())) {
                        Optional.ofNullable(updateQAItemRequest.getAnswer()).ifPresent(item1::setAnswer);
                        Optional.ofNullable(updateQAItemRequest.getQuestion()).ifPresent(item1::setQuestion);
                        item1.setSegmentation(SegmentUtil.segment(item1.getQuestion(), item1.getAnswer()));
                    }
                });
            }
            case KeyValueModule keyValueModule -> {
                UpdateNoteModuleItemRequest.UpdateKeyValueItemRequest updateKeyValueItemRequest = (UpdateNoteModuleItemRequest.UpdateKeyValueItemRequest) updateNoteModuleItemPayload;
                keyValueModule.getItems().forEach(item1 -> {
                    if (item1.getItemId().equals(request.getItemId())) {
                        Optional.ofNullable(updateKeyValueItemRequest.getKey()).ifPresent(item1::setKey);
                        Optional.ofNullable(updateKeyValueItemRequest.getValue()).ifPresent(item1::setValue);
                        item1.setSegmentation(SegmentUtil.segment(item1.getKey(), item1.getValue()));
                    }
                });
            }
            default -> throw new IllegalStateException("Unexpected value: " + module.getNoteModuleType());

        }
    }


    private void removeNoteModuleItem(NoteModule module, String itemId) {
        switch (module) {
            case AIRecommendationModule aiRecommendationModule ->
                    aiRecommendationModule.setItems(aiRecommendationModule.getItems().stream().filter(item1 -> !item1.getItemId().equals(itemId)).toList());
            case BacklogModule backlogModule ->
                    backlogModule.setItems(backlogModule.getItems().stream().filter(item1 -> !item1.getItemId().equals(itemId)).toList());
            case QAModule qaModule ->
                    qaModule.setItems(qaModule.getItems().stream().filter(item1 -> !item1.getItemId().equals(itemId)).toList());
            default -> throw new IllegalStateException("Unexpected value: " + module.getNoteModuleType());
        }
    }


}
