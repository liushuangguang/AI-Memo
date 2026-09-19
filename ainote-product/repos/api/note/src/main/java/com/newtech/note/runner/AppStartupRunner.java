package com.newtech.note.runner;

import com.newtech.note.service.common.RedisStreamObjectConsumer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AppStartupRunner implements CommandLineRunner {

    private final RedisStreamObjectConsumer streamConsumer;

    public AppStartupRunner(RedisStreamObjectConsumer streamConsumer) {
        this.streamConsumer = streamConsumer;
    }

    @Override
    public void run(String... args) {
        //streamConsumer.consumeMessages("yourStreamName","yourStreamName","yourStreamName");
    }
}