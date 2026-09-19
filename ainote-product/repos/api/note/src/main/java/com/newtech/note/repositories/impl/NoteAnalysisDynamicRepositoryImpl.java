package com.newtech.note.repositories.impl;

import com.newtech.note.entity.dto.NoteAnalysis;
import com.newtech.note.entity.filter.NoteAnalysisFilter;
import com.newtech.note.repositories.NoteAnalysisDynamicRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class NoteAnalysisDynamicRepositoryImpl implements NoteAnalysisDynamicRepository {
    private final DatabaseClient databaseClient;

    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    public NoteAnalysisDynamicRepositoryImpl(DatabaseClient databaseClient, R2dbcEntityTemplate r2dbcEntityTemplate) {
        this.databaseClient = databaseClient;
        this.r2dbcEntityTemplate = r2dbcEntityTemplate;
    }

    @Override
    public Flux<NoteAnalysis> findByDynamicCriteria(NoteAnalysisFilter filter, Pageable pageable) {
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM note_analysis WHERE 1=1");
        List<Object> parameters = builderParams(filter, queryBuilder);
        // 添加排序逻辑
        if (pageable.getSort().isSorted()) {
            queryBuilder.append(" ORDER BY ");
            pageable.getSort().forEach(order -> queryBuilder.append(order.getProperty())
                    .append(" ")
                    .append(order.isAscending() ? "ASC" : "DESC")
                    .append(", "));
            // 去掉最后的逗号
            queryBuilder.setLength(queryBuilder.length() - 2);
        }

        // 添加分页逻辑
        queryBuilder.append(" LIMIT ? OFFSET ?");
        parameters.add(pageable.getPageSize());
        parameters.add(pageable.getOffset());

        String query = queryBuilder.toString();
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(query);
        for (int i = 0; i < parameters.size(); i++) {
            spec = spec.bind(i, parameters.get(i));
        }
        return spec.map(row -> {
            NoteAnalysis entity = new NoteAnalysis();
            entity.setId(row.get("id", Long.class));
            entity.setDeviceId(row.get("device_id", String.class));
            entity.setRawNote(row.get("raw_note", String.class));
            entity.setNoteAnalysisContent(row.get("note_analysis_content", String.class));
            entity.setNoteType(row.get("note_type", Integer.class));
            entity.setTitle(row.get("title", String.class));
            entity.setTags(row.get("tags", String.class));
            entity.setTagList(Optional.ofNullable(entity.getTags()).stream().flatMap(tagStr -> Arrays.stream(tagStr.split(" "))).toList());
            entity.setCreatedAt(row.get("created_at", LocalDateTime.class));
            entity.setUpdatedAt(row.get("updated_at", LocalDateTime.class));
            //fixme 目前所有的时间我们都采用服务器时间保存到数据库，也就是说数据库的时间不再是UTC时间, 如果以后我们有业务可以走到海外，那么我们需要全部保存UTC时间，并由前端展示正确的时间。
            /*LocalDateTime createdAt = row.get("created_at", LocalDateTime.class);
            assert createdAt != null;
            entity.setCreatedAt(createdAt.atZone(ZoneId.of("UTC"))
                    .withZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime());
            LocalDateTime updatedAt = row.get("updated_at", LocalDateTime.class);
            assert updatedAt != null;
            entity.setUpdatedAt(updatedAt.atZone(ZoneId.of("UTC"))
                    .withZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime());*/
            entity.setDeletedAt(row.get("deleted_at", LocalDateTime.class));
            entity.setDeleted(row.get("is_deleted", Boolean.class));
            entity.setDimension(row.get("dimension", Integer.class));
            return entity;
        }).all();

    }

    private static List<Object> builderParams(NoteAnalysisFilter filter, StringBuilder queryBuilder) {
        List<Object> parameters = new ArrayList<>();
        if (filter.getDeviceId() != null) {
            queryBuilder.append(" AND device_id = ?");
            parameters.add(filter.getDeviceId());
        }
        if (filter.getNoteType() != null) {
            queryBuilder.append(" AND note_type = ?");
            parameters.add(filter.getNoteType());
        }
        if (StringUtils.isNotBlank(filter.getKeyword())) {
            queryBuilder.append(" AND MATCH(note_content_text) AGAINST(? IN BOOLEAN MODE)");
            parameters.add(filter.getKeyword());
        }
        if (filter.getDimension() != null) {
            queryBuilder.append(" AND dimension = ?");
            parameters.add(filter.getDimension());
        }
        queryBuilder.append(" AND is_deleted = 0");
        return parameters;
    }

    @Override
    public Mono<Long> countByDynamicCriteria(NoteAnalysisFilter filter) {
        StringBuilder queryBuilder = new StringBuilder("SELECT count(1) FROM note_analysis WHERE 1=1");
        List<Object> parameters = builderParams(filter, queryBuilder);
        String query = queryBuilder.toString();
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(query);
        for (int i = 0; i < parameters.size(); i++) {
            spec = spec.bind(i, parameters.get(i));
        }
        return spec.map(row -> row.get(0, Long.class))
                .one();
    }

    @Override
    public Mono<NoteAnalysis> insert(NoteAnalysis entity) {
        return r2dbcEntityTemplate.insert(entity);
    }
}
