package com.newtech.note.service.impl;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.common.NotePageData;
import com.newtech.note.entity.dto.Backlog;
import com.newtech.note.entity.filter.BacklogFilter;
import com.newtech.note.entity.request.CreateBacklogRequest;
import com.newtech.note.entity.request.SearchBacklogRequest;
import com.newtech.note.entity.request.UpdateBacklogRequest;
import com.newtech.note.repositories.BacklogRepository;
import com.newtech.note.service.BacklogService;
import com.newtech.note.util.SegmentUtil;
import com.newtech.note.util.SnowflakeIdGenerator;
import io.micrometer.common.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BacklogServiceImpl implements BacklogService {
    private static final Logger logger = LogManager.getLogger(BacklogServiceImpl.class);

    private final BacklogRepository backlogRepository;

    private final SnowflakeIdGenerator snowflakeIdGenerator;


    public BacklogServiceImpl(BacklogRepository backlogRepository) {
        this.backlogRepository = backlogRepository;
        this.snowflakeIdGenerator = SnowflakeIdGenerator.getInstance();
    }

    private static BacklogFilter buildFilter(String deviceId, SearchBacklogRequest request) {
        BacklogFilter noteFilter = request.buildFilter(deviceId);
        if (StringUtils.isNotBlank(request.getKeyword())) {
            List<Term> termList = HanLP.segment(request.getKeyword());
            String str = termList.stream().map(term -> term.word).filter(StringUtils::isNotBlank).collect(Collectors.joining(" "));
            noteFilter.setKeyword(str);
        }
        return noteFilter;
    }

    @Override
    public Mono<NoteBaseResponse<Backlog>> createBacklog(String deviceId, CreateBacklogRequest request) {
        logger.info("Creating backlog item");
        if (StringUtils.isBlank(request.getContent()) && request.getScheduledAt() == null) {
            return Mono.just(NoteBaseResponse.failure("代办事项内容和预定时间不能同时为空"));
        }
        if (request.getScheduledAt() != null && request.getScheduledAt().isBefore(LocalDateTime.now())) {
            return Mono.just(NoteBaseResponse.failure(("预定时间不能早于当前时间")));
        }
        Backlog backlog = new Backlog();
        backlog.setId(snowflakeIdGenerator.nextFullId(Backlog.class));
        backlog.setDeviceId(deviceId);
        backlog.setContent(request.getContent());
        backlog.setDescription(request.getDescription());
        backlog.setScheduledAt(request.getScheduledAt());
        backlog.setCreatedAt(LocalDateTime.now());
        if (StringUtils.isNotBlank(request.getContent()) || StringUtils.isNotBlank(request.getDescription())) {
            backlog.setSegmentation(SegmentUtil.segment(request.getContent(), request.getDescription()));
        }
        backlog.setUpdatedAt(LocalDateTime.now());
        return backlogRepository.save(backlog).map(NoteBaseResponse::success);
        //.flatMap(inserted -> noteRepository.findById(inserted.getId()));
    }

    @Override
    public Mono<Backlog> getBacklogById(String deviceId, String id) {
        return backlogRepository.findById(id)
                .filter(backlog -> backlog.getDeviceId().equals(deviceId));
    }

    @Override
    public Flux<Backlog> listBacklog(String deviceId, SearchBacklogRequest request) {
        logger.info("Listing backlog items");
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize(), request.isAscending() ? Sort.by(request.getSortBy()).ascending() : Sort.by(request.getSortBy()).descending());
        BacklogFilter backlogFilter = buildFilter(deviceId, request);
        return backlogRepository.findByDynamicCriteria(backlogFilter, pageable);
    }

    @Override
    public Mono<NoteBaseResponse<NotePageData<Backlog>>> backlogPagination(String deviceId, SearchBacklogRequest request) {
        logger.info("Listing backlog items with pagination");
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize(), request.isAscending() ? Sort.by(request.getSortBy()).ascending() : Sort.by(request.getSortBy()).descending());

        // 将总数、分页数据和元数据打包成自定义的 PageData 对象
        return count(deviceId, request)
                .flatMap(total -> {
                    if (total == 0L) {
                        return Mono.just(NoteBaseResponse.success());
                    }
                    long totalPages = (total + request.getSize() - 1) / request.getSize();  // 计算总页数
                    BacklogFilter noteFilter = buildFilter(deviceId, request);
                    return backlogRepository.findByDynamicCriteria(noteFilter, pageable)
                            .collectList()
                            .map(list -> NoteBaseResponse.success(new NotePageData<>(list, request.getPage(), totalPages, total)));
                });
    }

    @Override
    public Mono<NoteBaseResponse<Backlog>> updateBacklog(String deviceId, UpdateBacklogRequest request) {
        return backlogRepository.findById(request.getId())
                .flatMap(exists -> {
                    if (!exists.getDeviceId().equals(deviceId)) {
                        logger.error("Backlog update denied because ownership did not match");
                        return Mono.just(NoteBaseResponse.failure("Failed to update backlog , not same device id."));
                    }
                    if (request.getScheduledAt() != null) {
                        exists.setScheduledAt(request.getScheduledAt());
                    }
                    if (request.getIsDone() != null) {
                        exists.setDone(request.getIsDone());
                    }
                    if (request.getIsNeedNotify() != null) {
                        exists.setNeedNotify(request.getIsNeedNotify());
                    }
                    if (request.getContent() != null) {
                        exists.setContent(request.getContent());
                    }
                    if (request.getDescription() != null) {
                        exists.setDescription(request.getDescription());
                    }
                    if (request.getContent() != null || request.getDescription() != null) {
                        exists.setSegmentation(SegmentUtil.segment(request.getContent(),request.getDescription()));
                    }
                    exists.setUpdatedAt(LocalDateTime.now());
                    return backlogRepository.save(exists).map(NoteBaseResponse::success);
                });
    }

    @Override
    public Mono<NoteBaseResponse<Backlog>> deleteBacklog(String deviceId, String id) {
        return backlogRepository.findById(id)
                .flatMap(exists -> {
                    if (!exists.getDeviceId().equals(deviceId)) {
                        logger.error("Backlog deletion denied because ownership did not match");
                        return Mono.just(NoteBaseResponse.failure("无法删除别人创建的代办事项"));
                    }
                    exists.setDeleted(true);
                    exists.setDeletedAt(LocalDateTime.now());
                    return backlogRepository.save(exists).map(NoteBaseResponse::success);
                });
    }

    @Override
    public Mono<Long> count(String deviceId, SearchBacklogRequest request) {
        BacklogFilter backlogFilter = buildFilter(deviceId, request);
        return backlogRepository.countByDynamicCriteria(backlogFilter);
    }
}
