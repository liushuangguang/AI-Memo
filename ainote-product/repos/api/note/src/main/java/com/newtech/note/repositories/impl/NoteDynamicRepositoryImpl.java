package com.newtech.note.repositories.impl;

import com.mongodb.BasicDBObject;
import com.newtech.note.entity.dto.Note;
import com.newtech.note.entity.filter.NoteFilterV2;
import com.newtech.note.repositories.NoteDynamicRepository;
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
public class NoteDynamicRepositoryImpl implements NoteDynamicRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public NoteDynamicRepositoryImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<Note> findByDynamicCriteria(NoteFilterV2 filter, Pageable pageable) {
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
        return mongoTemplate.find(query, Note.class);
    }

    @Override
    public Mono<Long> countByDynamicCriteria(NoteFilterV2 noteFilter) {
        Query query = new Query();
        // 动态添加查询条件
        List<Criteria> criteriaList = buildCriteriaList(noteFilter);
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }
        return mongoTemplate.count(query, Note.class);
    }

    // 动态构建查询条件
    private List<Criteria> buildCriteriaList(NoteFilterV2 filter) {
        List<Criteria> criteriaList = new ArrayList<>();
        assert filter.getDeviceId() != null;
        criteriaList.add(Criteria.where("deviceId").is(filter.getDeviceId()));
        criteriaList.add(Criteria.where("deleted").is(false));
        if (filter.getNoteType() != null) {
            criteriaList.add(Criteria.where("noteType").is(filter.getNoteType()));
        }
        if (filter.getDimension() != null) {
            criteriaList.add(Criteria.where("dimension").is(filter.getDimension()));
        }
        if (StringUtils.isNotBlank(filter.getKeyword())) {
            criteriaList.add(Criteria.where("$text").is(new BasicDBObject("$search", filter.getKeyword())));
        }
        // 可以根据 filter 添加其他动态查询条件
        return criteriaList;
    }
}
