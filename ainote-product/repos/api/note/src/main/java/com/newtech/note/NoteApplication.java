package com.newtech.note;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = "com.newtech.note")
@OpenAPIDefinition(info = @Info(title = "Note API", version = "1.0.0", description = "Note API"))
public class NoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(NoteApplication.class, args);
        //创建请求处理器
        /*HttpHandler handler = (ServerHttpRequest request, ServerHttpResponse response) -> {
            //处理请求
            System.out.println(Thread.currentThread().getName() + " Received request: " + request.getURI());
            //测试普通响应
            *//*DataBufferFactory bufferFactory = response.bufferFactory();
            List<String> messages = List.of("Hello, World!", "Hello, ketty!", "Hello, john!");
            Flux<DataBuffer> flux = Flux.fromIterable(messages)
                    .map(message -> bufferFactory.wrap(message.getBytes()))
                    .delayElements(Duration.ofSeconds(1)); // 每隔1秒发送一个消息
            return response.writeWith(flux);*//*

            //测试SSE响应
            *//*response.setStatusCode(HttpStatusCode.valueOf(200));
            response.getHeaders().setContentType(MediaType.TEXT_EVENT_STREAM);

            Flux<ServerSentEvent<String>> eventFlux = Flux.interval(Duration.ofSeconds(1))
                    .map(sequence -> ServerSentEvent.<String>builder()
                            .id(String.valueOf(sequence))
                            .event("periodic-event")
                            .data("SSE - " + sequence)
                            .build());

            return response.writeWith(eventFlux.map(event -> response.bufferFactory().wrap(event.toString().getBytes())));*//*
        };*/

        //ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(handler);
        //启动一个服务器，监听8081端口
        //HttpServer.create().host("localhost").port(8081).handle(new ReactorHttpHandlerAdapter(handler)).bindNow();
    }

}
