package com.newtech.note.repositories.impl;

import com.mongodb.BasicDBObject;
import com.newtech.note.entity.dto.NoteTheme;
import com.newtech.note.entity.filter.NoteThemeFilter;
import com.newtech.note.repositories.NoteThemeDynamicRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class NoteThemeDynamicRepositoryImpl implements NoteThemeDynamicRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public NoteThemeDynamicRepositoryImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<NoteTheme> findByDynamicCriteria(NoteThemeFilter filter, Pageable pageable) {
        // 构建查询
        Query query = new Query();
        // 动态添加查询条件
        List<Criteria> criteriaList = buildCriteriaList(filter);
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }
        // 添加分页和排序
        if (pageable != null) {
            query.with(pageable);
        }
        // 执行查询并返回响应式流
        return mongoTemplate.find(query, NoteTheme.class);
    }

    @Override
    public Mono<Long> countByDynamicCriteria(NoteThemeFilter noteThemeFilter) {
        Query query = new Query();
        // 动态添加查询条件
        List<Criteria> criteriaList = buildCriteriaList(noteThemeFilter);
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }
        return mongoTemplate.count(query, NoteTheme.class);
    }

    // 动态构建查询条件
    private List<Criteria> buildCriteriaList(NoteThemeFilter filter) {
        List<Criteria> criteriaList = new ArrayList<>();
        assert filter.getDeviceId() != null;
        criteriaList.add(Criteria.where("deviceId").is(filter.getDeviceId()));
        criteriaList.add(Criteria.where("deleted").is(false));
        if (StringUtils.isNotBlank(filter.getKeyword())) {
            criteriaList.add(Criteria.where("$text").is(new BasicDBObject("$search", filter.getKeyword())));
        }
        // 可以根据 filter 添加其他动态查询条件
        return criteriaList;
    }
}
