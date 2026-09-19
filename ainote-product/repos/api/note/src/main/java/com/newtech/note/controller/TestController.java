package com.newtech.note.controller;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.entity.request.AnalyzeTextContentRequest;
import com.newtech.note.service.ImageGenService;
import com.newtech.note.service.impl.DifyNoteServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/test")
@AllArgsConstructor
@Slf4j
@Tag(name = "测试处理器", description = "用于做测试")
public class TestController {
    private static final Logger logger = LogManager.getLogger(TestController.class);
    private final ImageGenService imageGenService;
    private final DifyNoteServiceImpl difyNoteService;

    //sse 意味着服务器发送事件，这里返回的是一个Flux<ServerSentEvent> 类型，可以用来实现服务器推送消息
    @GetMapping(value = "/sse/test", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "测试SSE",
            description = "测试Server-Sent-Event")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {@Content(schema = @Schema(implementation = String.class), mediaType = "application/text")}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema())}),
            @ApiResponse(responseCode = "500", content = {@Content(schema = @Schema())})})
    public Flux<String> testSse() {
        return Flux.range(0, 20)
                .map(num -> "num " + num + " is prepared")
                .delayElements(Duration.ofSeconds(1));
    }

    @GetMapping(value = "/log/test", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "测试日志",
            description = "测试日志输出")

    public Mono<String> testLog() {
        return Mono.just("测试日志输出，是否出现中文乱码问题").log().map(log -> {
            logger.info(log);
            return log;
        });
    }

    /**
     * 测试coze非流式token记录
     */
    @GetMapping(value = "/test2")
    public Mono<NoteBaseResponse<String>> test2(AnalyzeTextContentRequest request, ServerHttpRequest req) {
        Mono<NoteBaseResponse<String>> map = imageGenService.gen(request.getSpecificContent(), req)
                .map(NoteBaseResponse::success);
        log.error("map:{}", map.toString());
        return map;
    }

    /**
     * 测试dify非流式token记录
     */
    @GetMapping(value = "/test3")
    public Mono<NoteBaseResponse<String>> test3(String request, ServerHttpRequest req) {
        return difyNoteService.isNoteMakeSense(request,req).map(NoteBaseResponse::success);
    }

    /**
     * 测试dify流式token记录
     */
    @GetMapping(value = "/note/analysis/todo")
    public Flux<NoteBaseResponse<String>> test4(String request, ServerHttpRequest req) {
        Flux<String> stringFlux = difyNoteService.aiSuggestion(request, req);
        log.info("test4 :{}",stringFlux.toString());
        return difyNoteService.aiSuggestion(request,req).map(NoteBaseResponse::success);
    }
}
