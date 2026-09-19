package com.newtech.note.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.File;
import java.nio.ByteBuffer;

public interface AudioSenderService {
    public Flux<String> sendAudio(File file);

    public void receiveAudio(String text, Sinks.Many<ByteBuffer> sinkMany);
}
