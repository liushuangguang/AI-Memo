package com.newtech.note.controller;

import com.newtech.note.entity.dto.NoteAssistRecord;
import com.newtech.note.entity.request.CreateNoteAssistRecordRequest;
import com.newtech.note.service.NoteAssistRecordService;
import com.newtech.note.security.NoteOwnershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/note/assist/record")
@Tag(name = "笔记辅助历史记录处理器", description = "Note辅助历史记录处理器，用于获得当前note的辅助历史记录,最新历史记录等")
public class NoteAssistRecordController {

    private final NoteAssistRecordService noteAssistRecordService;
    private final NoteOwnershipService ownershipService;

    public NoteAssistRecordController(NoteAssistRecordService noteAssistRecordService,
                                      NoteOwnershipService ownershipService) {
        this.noteAssistRecordService = noteAssistRecordService;
        this.ownershipService = ownershipService;
    }

    @Operation(
            summary = "创建笔记辅助记录",
            description = "创建笔记辅助记录, 在创建NoteAssist时会自动创建NoteAssistRecord")
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public Mono<NoteAssistRecord> createAssist(@RequestBody CreateNoteAssistRecordRequest request,
                                               ServerHttpRequest httpRequest) {
        return ownershipService.ownedNote(request.getNoteId(), httpRequest)
                .then(noteAssistRecordService.create(request));
    }

    @Operation(
            summary = "查看某个note的最新AI辅助历史记录",
            description = "查看某个note的最新AI辅助历史记录")
    @ResponseBody
    @GetMapping(value = "/latest")
    public Mono<NoteAssistRecord> latestAssist(@Schema(description = "notes的id") @RequestParam("noteId") String noteId,
                                               ServerHttpRequest request) {
        return ownershipService.ownedNote(noteId, request)
                .then(noteAssistRecordService.latestAssist(noteId));
    }

    @Operation(
            summary = "查看指定版本号下的note辅助历史记录",
            description = "查看指定版本号下的note辅助历史记录")
    @ResponseBody
    @GetMapping(value = "/specific")
    public Mono<NoteAssistRecord> specificAssist(@Schema(description = "notes集合的id") @RequestParam("noteId") String noteId,
                                                 @RequestParam("version") int version,
                                                 ServerHttpRequest request) {
        return ownershipService.ownedNote(noteId, request)
                .then(noteAssistRecordService.specificAssist(noteId, version));
    }

    @Operation(
            summary = "查看一个note的所有的AI辅助记录",
            description = "查看一个note的所有的AI辅助记录")
    @ResponseBody
    @RequestMapping(value = "/all", method = RequestMethod.POST, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<NoteAssistRecord> allAssistRecords(@Schema(description = "notes集合的id") @RequestParam("noteId") String noteId,
                                                   ServerHttpRequest request) {
        return ownershipService.ownedNote(noteId, request)
                .thenMany(noteAssistRecordService.allAssistRecords(noteId));
    }
}
