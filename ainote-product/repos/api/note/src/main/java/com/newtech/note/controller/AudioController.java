package com.newtech.note.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.newtech.note.entity.dto.NoteAnalysisHistory;
import com.newtech.note.entity.request.DeleteNoteDiscussAudioRequest;
import com.newtech.note.entity.request.GetNoteDiscussAudioRequest;
import com.newtech.note.entity.request.OrganizeToDoListNoteRequest;
import com.newtech.note.entity.request.UpdateNoteDiscussAudioRequest;
import com.newtech.note.service.AudioSenderService;
import com.newtech.note.service.AudioSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/audio")
@Tag(name = "音频控制器", description = "音频识别、语音转文字等控制模块")
@Slf4j
public class AudioController {
    private final AudioSessionService audioSessionService;

    private final AudioSenderService audioSenderService;

//    @Autowired
//    private Sinks.Many<ByteBuffer> sink;

    public AudioController(AudioSessionService audioSessionService, AudioSenderService audioSenderService) {
        this.audioSessionService = audioSessionService;
        this.audioSenderService = audioSenderService;
    }

    @PostMapping("/recognize")
    @Operation(summary = "识别音频", description = "接收 pcm 格式的音频，上传文件大小不得超过5MB")
    public Flux<String> parseAudio(@RequestPart("file") FilePart uploadFile) {

        return Flux.create(sink -> {
            try {
                // 将 FilePart 保存到临时文件
                Path tempFile = Files.createTempFile("speech_", "_" + uploadFile.filename());
                File targetFile = tempFile.toFile();

                // 异步保存文件
                // 处理文件保存错误
                uploadFile.transferTo(targetFile)
                        .doOnSuccess(v -> {
                            // 调用 sendAudioAsFlux 方法
                            Flux<String> resultFlux = audioSenderService.sendAudio(targetFile);
                            resultFlux.subscribe(
                                    sink::next,    // 推送每个中间结果
                                    sink::error,   // 处理错误
                                    sink::complete // 完成
                            );
                        })
                        .doOnError(sink::error)
                        .subscribe();
            } catch (Exception e) {
                sink.error(e); // 处理异常
            }
        });
    }

    @GetMapping("/tts/{text}")
    @Operation(summary = "文字转语音", description = "返回 mp3 格式音频文件，字数不超过 2000")
    public ResponseEntity<Flux<DataBuffer>> parseAudio(@PathVariable String text) throws InterruptedException {
        Sinks.Many<ByteBuffer> sink = Sinks.many().multicast().onBackpressureBuffer();
        audioSenderService.receiveAudio(text, sink);
        DataBufferFactory bufferFactory = new DefaultDataBufferFactory();
        Flux<DataBuffer> mp3Stream = sink.asFlux()
                .map(bufferFactory::wrap);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"streamed_audio.mp3\"")
                .contentType(new MediaType("audio", "mpeg"))
                .body(mp3Stream);
    }

    @Operation(summary = "获得Bot针对语音文本笔记回复", description = "通过coze对流式语音尝试Bot，返回内容")
    @ResponseBody
    @PostMapping("/getOrganizeToDoListNote")
    public Mono<String> getOrganizeToDoListNote(@RequestBody OrganizeToDoListNoteRequest request) {
        return audioSessionService.getOrganizeToDoListNote(request);
    }

    @Operation(summary = "通过历史版本ID手动保存语音讨论笔记版本历史内容", description = "通过历史版本ID手动保存语音讨论笔记版本历史内容")
    @ResponseBody
    @PostMapping("/updateNoteDiscussAudio")
    public Mono<NoteAnalysisHistory> updateNoteDiscussAudio(@RequestBody UpdateNoteDiscussAudioRequest request) {
        return audioSessionService.updateNoteDiscussAudio(request);
    }

    @Operation(summary = "2. 获取笔记详情需要查询会议的语音讨论", description = "获取笔记详情需要查询会议的语音讨论")
    @ResponseBody
    @PostMapping("/getNoteDiscussAudio")
    public Mono<JsonNode> getNoteDiscussAudio(@RequestBody GetNoteDiscussAudioRequest request) {
        return audioSessionService.getNoteDiscussAudio(request);
    }

    @Operation(summary = "3. 删除语音讨论", description = "删除语音讨论")
    @ResponseBody
    @PostMapping("/deleteNoteDiscussAudio")
    public Mono<Integer> deleteNoteDiscussAudio(@RequestBody DeleteNoteDiscussAudioRequest request) {
        return audioSessionService.deleteNoteDiscussAudio(request);
    }

    @Operation(summary = "语音讨论完毕之后 ai 总结", description = "语音讨论完毕之后 ai 总结")
    @ResponseBody
    @PostMapping("/noteDiscussAudioAiSummary")
    public Mono<String> noteDiscussAudioAiSummary(@RequestBody JsonNode talkAudio) {
        return audioSessionService.noteDiscussAudioAiSummary(talkAudio);
    }

}
