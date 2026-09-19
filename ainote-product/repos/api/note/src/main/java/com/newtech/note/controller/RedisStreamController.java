package com.newtech.note.controller;

import com.newtech.note.entity.pojo.NoteId;
import com.newtech.note.service.common.NoteAutoOrganizeProducer;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Hidden
@RestController
@RequestMapping("/redis-stream")
@Tag(name = "redis stream 处理器", description = "用于做测试")
public class RedisStreamController {
    private final NoteAutoOrganizeProducer producer;


    public RedisStreamController(NoteAutoOrganizeProducer producer) {
        this.producer = producer;
    }

    @GetMapping("/send")
    public Mono<String> sendMessage(@RequestParam String id) {
        NoteId noteId = new NoteId(id);
        return producer.sendMessageToStream(noteId);
    }
}
