package com.newtech.note.service.impl;

import com.newtech.note.entity.dto.TokenRecord;
import com.newtech.note.repositories.TokenRecordRepository;
import com.newtech.note.service.TokenRecordService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class TokenRecordServiceImpl implements TokenRecordService {

    private final TokenRecordRepository tokenRecordRepository;

    @Override
    public Mono<Long> saveTokenRecord(String deviceId, Long token) {
        TokenRecord tokenRecord = new TokenRecord();
        tokenRecord.setDeviceId(deviceId);
        tokenRecord.setToken(token);
        return tokenRecordRepository.saveTokenByDeviceId(tokenRecord);
    }
}
