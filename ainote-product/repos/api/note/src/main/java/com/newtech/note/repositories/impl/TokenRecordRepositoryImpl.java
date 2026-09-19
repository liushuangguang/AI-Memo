package com.newtech.note.repositories.impl;

import com.newtech.note.entity.dto.TokenRecord;
import com.newtech.note.repositories.TokenRecordRepository;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TokenRecordRepositoryImpl implements TokenRecordRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public TokenRecordRepositoryImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Long> saveTokenByDeviceId(TokenRecord tokenRecord) {
        Query query = Query.query(Criteria.where("_id").is(tokenRecord.getDeviceId()));
        Update update = new Update().inc("token", tokenRecord.getToken());
        return mongoTemplate.updateFirst(query, update, TokenRecord.class)
                .flatMap(result -> {
                    if (result.getModifiedCount() == 0) {
                        // 如果文档不存在，创建新文档
                        return mongoTemplate.insert(tokenRecord)
                                .then(Mono.just(tokenRecord.getToken()));
                    } else {
                        // 如果文档已经存在，返回累加后的 token 值
                        return mongoTemplate.findById(tokenRecord.getDeviceId(), TokenRecord.class)
                                .map(TokenRecord::getToken);
                    }
                });
    }
}
