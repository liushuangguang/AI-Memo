package com.newtech.note.service;

import reactor.core.publisher.Mono;

public interface TokenRecordService {
    Mono<Long> saveTokenRecord(String deviceId, Long token);
}
