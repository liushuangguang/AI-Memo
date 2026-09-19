package com.newtech.note.repositories;

import com.newtech.note.entity.dto.TokenRecord;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface TokenRecordRepository{
    Mono<Long> saveTokenByDeviceId(TokenRecord tokenRecord);

}
