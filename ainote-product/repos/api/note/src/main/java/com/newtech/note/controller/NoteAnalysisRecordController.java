package com.newtech.note.controller;

import com.newtech.note.entity.dto.NoteAnalysisRecord;
import com.newtech.note.entity.request.CreateNoteAnalysisRecordRequest;
import com.newtech.note.service.NoteAnalysisRecordService;
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
@RequestMapping("/note/analysis/record")
@Tag(name = "笔记分析历史记录处理器", description = "Note分析历史记录处理器，用于获得当前note的分析历史记录,最新历史记录等")
public class NoteAnalysisRecordController {

    private final NoteAnalysisRecordService noteAnalysisRecordService;
    private final NoteOwnershipService ownershipService;

    public NoteAnalysisRecordController(NoteAnalysisRecordService noteAnalysisRecordService,
                                        NoteOwnershipService ownershipService) {
        this.noteAnalysisRecordService = noteAnalysisRecordService;
        this.ownershipService = ownershipService;
    }

    @Operation(
            summary = "创建笔记分析记录",
            description = "创建笔记分析记录, 在创建NoteAnalysis时会自动创建NoteAnalysisRecord")
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public Mono<NoteAnalysisRecord> createAnalysis(@RequestBody CreateNoteAnalysisRecordRequest request,
                                                   ServerHttpRequest httpRequest) {
        return ownershipService.ownedNote(request.getNoteId(), httpRequest)
                .then(noteAnalysisRecordService.create(request));
    }

    @Operation(
            summary = "查看某个note的最新AI分析历史记录",
            description = "查看某个note的最新AI分析历史记录")
    @ResponseBody
    @GetMapping(value = "/latest")
    public Mono<NoteAnalysisRecord> latestAnalysis(@Schema(description = "notes的id") @RequestParam("noteId") String noteId,
                                                   ServerHttpRequest request) {
        return ownershipService.ownedNote(noteId, request)
                .then(noteAnalysisRecordService.latestAnalysis(noteId));
    }

    @Operation(
            summary = "查看指定版本号下的note分析历史记录",
            description = "查看指定版本号下的note分析历史记录")
    @ResponseBody
    @GetMapping(value = "/specific")
    public Mono<NoteAnalysisRecord> specificAnalysis(@Schema(description = "notes集合的id") @RequestParam("noteId") String noteId,
                                                     @RequestParam("version") int version,
                                                     ServerHttpRequest request) {
        return ownershipService.ownedNote(noteId, request)
                .then(noteAnalysisRecordService.specificAnalysis(noteId, version));
    }

    @Operation(
            summary = "查看一个note的所有的AI整理记录",
            description = "查看一个note的所有的AI整理记录")
    @ResponseBody
    @RequestMapping(value = "/all", method = RequestMethod.POST, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<NoteAnalysisRecord> allAnalysisRecords(@Schema(description = "notes集合的id") @RequestParam("noteId") String noteId,
                                                       ServerHttpRequest request) {
        return ownershipService.ownedNote(noteId, request)
                .thenMany(noteAnalysisRecordService.allAnalysisRecords(noteId));
    }
}
