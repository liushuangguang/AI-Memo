package com.newtech.note.service.impl;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.newtech.note.client.EmbeddingClient;
import com.newtech.note.client.entity.embedding.UpsertThemeReq;
import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.common.NotePageData;
import com.newtech.note.entity.dto.NoteTheme;
import com.newtech.note.entity.filter.NoteThemeFilter;
import com.newtech.note.entity.request.CreateNoteThemeRequest;
import com.newtech.note.entity.request.SearchNoteThemeRequest;
import com.newtech.note.entity.request.UpdateNoteThemeRequest;
import com.newtech.note.repositories.NoteThemeRepository;
import com.newtech.note.service.NoteEmbeddingService;
import com.newtech.note.service.NoteServiceV2;
import com.newtech.note.service.NoteThemeService;
import com.newtech.note.util.SegmentUtil;
import com.newtech.note.util.SnowflakeIdGenerator;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NoteThemeServiceImpl implements NoteThemeService {
    private static final int MAX_THEME_LENGTH = 120;
    private static final int MAX_DESCRIPTION_LENGTH = 1_000;
    private final NoteThemeRepository noteThemeRepository;
    private final NoteEmbeddingService noteEmbeddingService;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    private final NoteServiceV2 noteService;

    private final EmbeddingClient innerEmbeddingClient;
    private final ReactiveMongoTemplate mongoTemplate;

    public NoteThemeServiceImpl(NoteThemeRepository noteThemeRepository,
                                NoteEmbeddingService noteEmbeddingService,
                                NoteServiceV2 noteService,
                                EmbeddingClient innerEmbeddingClient,
                                ReactiveMongoTemplate mongoTemplate) {
        this.noteThemeRepository = noteThemeRepository;
        this.noteEmbeddingService = noteEmbeddingService;
        this.noteService = noteService;
        this.innerEmbeddingClient = innerEmbeddingClient;
        this.mongoTemplate = mongoTemplate;
        this.snowflakeIdGenerator = SnowflakeIdGenerator.getInstance();

    }


    @Override
    public Mono<NoteBaseResponse<NoteTheme>> createNoteTheme(String deviceId, CreateNoteThemeRequest request) {
        log.info("Creating note theme");
        if (request == null || StringUtils.isBlank(request.getTheme())) {
            return Mono.just(NoteBaseResponse.failure("备忘录主题不能为空"));
        }
        String theme = request.getTheme().trim();
        String description = request.getDescription() == null ? "" : request.getDescription().trim();
        if (theme.length() > MAX_THEME_LENGTH
                || description.length() > MAX_DESCRIPTION_LENGTH) {
            return Mono.just(NoteBaseResponse.failure("主题或描述超出允许长度"));
        }
        NoteTheme noteTheme = new NoteTheme();
        noteTheme.setId(snowflakeIdGenerator.nextFullId(NoteTheme.class));
        noteTheme.setDeviceId(deviceId);
        noteTheme.setTheme(theme);
        noteTheme.setDescription(description);
        noteTheme.setSegmentation(SegmentUtil.segment(theme, description));
        noteTheme.setMergeHistory(new ArrayList<>());
        noteTheme.setCreatedAt(LocalDateTime.now());
        noteTheme.setUpdatedAt(LocalDateTime.now());
        // Candidate discovery is now an explicit, user-visible AI step. Theme creation must
        // not silently write associations into candidate source notes.
        return noteThemeRepository.save(noteTheme).map(NoteBaseResponse::success);
    }

    @Override
    public Mono<NoteBaseResponse<NoteTheme>> updateNoteTheme(String deviceId, UpdateNoteThemeRequest request) {
        if (request == null || StringUtils.isBlank(request.getId())) {
            return Mono.just(NoteBaseResponse.failure("主题不存在"));
        }
        return noteThemeRepository.findById(request.getId())
                .flatMap(exists -> {
                    if (!exists.getDeviceId().equals(deviceId) || exists.isDeleted()) {
                        log.error("Failed to update note , not same device id {}, the note id is {}", deviceId, request.getId());
                        return Mono.just(NoteBaseResponse.failure("不能更新别人创建的备忘录主题"));
                    }
                    String nextTheme = exists.getTheme();
                    String nextDescription = exists.getDescription();
                    Update update = new Update();
                    if (request.getTheme() != null) {
                        String theme = request.getTheme().trim();
                        if (theme.isEmpty() || theme.length() > MAX_THEME_LENGTH) {
                            return Mono.just(NoteBaseResponse.<NoteTheme>failure("主题内容无效"));
                        }
                        nextTheme = theme;
                        update.set("theme", theme);
                    }
                    if (request.getDescription() != null) {
                        String description = request.getDescription().trim();
                        if (description.length() > MAX_DESCRIPTION_LENGTH) {
                            return Mono.just(NoteBaseResponse.<NoteTheme>failure("主题描述过长"));
                        }
                        nextDescription = description;
                        update.set("description", description);
                    }
                    update.set("segmentation", SegmentUtil.segment(nextTheme, nextDescription));
                    update.set("updatedAt", LocalDateTime.now());
                    Query query = Query.query(Criteria.where("_id").is(exists.getId())
                            .and("deviceId").is(deviceId)
                            .and("deleted").is(false));
                    return mongoTemplate.updateFirst(query, update, NoteTheme.class)
                            .flatMap(result -> {
                                if (result.getMatchedCount() != 1) {
                                    return Mono.just(NoteBaseResponse.<NoteTheme>failure("主题已不存在"));
                                }
                                return noteThemeRepository.findById(exists.getId())
                                        .map(NoteBaseResponse::success)
                                        .switchIfEmpty(Mono.just(
                                                NoteBaseResponse.failure("主题已不存在")));
                            });
                });
    }

    @Override
    public Mono<NoteBaseResponse<NotePageData<NoteTheme>>> noteThemePagination(String deviceId, SearchNoteThemeRequest request) {
        log.info("Listing note themes");
        if (request == null) request = new SearchNoteThemeRequest();
        request.setPage(Math.max(1, request.getPage() == null ? 1 : request.getPage()));
        request.setSize(Math.max(1, Math.min(50, request.getSize() == null ? 10 : request.getSize())));
        if (!List.of("id", "updatedAt").contains(request.getSortBy())) {
            request.setSortBy("updatedAt");
        }
        SearchNoteThemeRequest boundedRequest = request;
        Pageable pageable = PageRequest.of(boundedRequest.getPage() - 1, boundedRequest.getSize(), boundedRequest.isAscending() ? Sort.by(boundedRequest.getSortBy()).ascending() : Sort.by(boundedRequest.getSortBy()).descending());
        // 将总数、分页数据和元数据打包成自定义的 PageData 对象
        return count(deviceId, boundedRequest)
                .flatMap(total -> {
                    if (total == 0L) {
                        return Mono.just(NoteBaseResponse.<NotePageData<NoteTheme>>success());
                    }
                    long totalPages = (total + boundedRequest.getSize() - 1) / boundedRequest.getSize();
                    NoteThemeFilter noteThemeFilter = buildFilter(deviceId, boundedRequest);
                    return noteThemeRepository.findByDynamicCriteria(noteThemeFilter, pageable)
                            .collectList()
                            .map(list -> NoteBaseResponse.success(new NotePageData<>(list, boundedRequest.getPage(), totalPages, total)));
                }).onErrorResume((error) -> {
                    log.error("Failed to list note, the error", error);
                    return Mono.just(NoteBaseResponse.failure("无法获取备忘录主题列表, 错误原因是: " + error.getMessage()));
                });
    }

    @Override
    public Mono<Long> count(String deviceId, SearchNoteThemeRequest request) {
        NoteThemeFilter noteThemeFilter = buildFilter(deviceId, request);
        return noteThemeRepository.countByDynamicCriteria(noteThemeFilter);
    }

    @Override
    public Mono<NoteBaseResponse<NoteTheme>> deleteNoteTheme(String deviceId, String id) {
        return noteThemeRepository.findById(id)
                .flatMap(exists -> {
                    if (!exists.getDeviceId().equals(deviceId)) {
                        log.error("Failed to delete note , not same device id {}, the note id is {}", deviceId, id);
                        return Mono.just(NoteBaseResponse.failure("不能删除别人创建的备忘录主题"));
                    }
                    LocalDateTime deletedAt = LocalDateTime.now();
                    Query query = Query.query(Criteria.where("_id").is(id)
                            .and("deviceId").is(deviceId)
                            .and("deleted").is(false));
                    Update update = new Update()
                            .set("deleted", true)
                            .set("deletedAt", deletedAt)
                            .set("updatedAt", deletedAt);
                    return mongoTemplate.updateFirst(query, update, NoteTheme.class)
                            .flatMap(result -> {
                                if (result.getMatchedCount() != 1) {
                                    return Mono.just(NoteBaseResponse.<NoteTheme>failure("主题已不存在"));
                                }
                                exists.setDeleted(true);
                                exists.setDeletedAt(deletedAt);
                                exists.setUpdatedAt(deletedAt);
                                return Mono.just(NoteBaseResponse.success(exists));
                            });
                });
    }

    @Override
    public Mono<NoteTheme> findThemeById(String noteThemeId) {
        return noteThemeRepository.findById(noteThemeId);
    }

    private static NoteThemeFilter buildFilter(String deviceId, SearchNoteThemeRequest request) {
        NoteThemeFilter noteThemeFilter = request.buildFilter(deviceId);
        if (StringUtils.isNotBlank(request.getKeyword())) {
            List<Term> termList = HanLP.segment(request.getKeyword());
            String str = termList.stream().map(term -> term.word).filter(StringUtils::isNotBlank).collect(Collectors.joining(" "));
            noteThemeFilter.setKeyword(str);
        }
        return noteThemeFilter;
    }

    public Mono<Void> themeEmbedding(String noteThemeId) {
        return this.findThemeById(noteThemeId)
                .flatMap(theme ->
                        innerEmbeddingClient.upsertTheme(new UpsertThemeReq(theme.getDeviceId(), theme.getId(), theme.getTheme() + " " + theme.getDescription()))
                                .flatMap(res -> noteService.addNotesThemeId(res.related_note_ids(), theme.getId()))
                                .then()
                );
    }
}
