package com.newtech.note.service;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ResponseEntity;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

public interface IFlytekRealtimeTtsService {

    Mono<ResponseEntity<DataBuffer>> convert(String text) throws RuntimeException, InterruptedException;

}