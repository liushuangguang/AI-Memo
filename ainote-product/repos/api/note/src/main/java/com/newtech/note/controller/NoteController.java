package com.newtech.note.controller;

import com.newtech.note.entity.dto.NoteAnalysis;
import com.newtech.note.entity.request.AppendToOriginalTextRequest;
import com.newtech.note.entity.request.CreateNoteAnalysisRequest;
import com.newtech.note.entity.request.SearchNoteRequest;
import com.newtech.note.entity.request.UpdateNoteAnalysisRequest;
import com.newtech.note.service.NoteService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
@Hidden
@RestController
@RequestMapping("/note")
@Tag(name = "笔记CURD处理器", description = "Note处理器，用于创建、更新、删除、查询笔记")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @Operation(
            summary = "获得所有笔记列表",
            description = "查询当前设备下所有笔记列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {@Content(schema = @Schema(implementation = NoteAnalysis.class), mediaType = "application/json")}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema())}),
            @ApiResponse(responseCode = "500", content = {@Content(schema = @Schema())})})
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public Flux<NoteAnalysis> getNoteList(@RequestHeader("device-id") String deviceId, @RequestBody SearchNoteRequest request) {
        return noteService.listNote(deviceId, request);
    }


    @Operation(
            summary = "获得单个笔记详情",
            description = "获得单个笔记详情")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {@Content(schema = @Schema(implementation = NoteAnalysis.class), mediaType = "application/json")}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema())}),
            @ApiResponse(responseCode = "500", content = {@Content(schema = @Schema())})})
    @GetMapping("/{id}")
    public Mono<NoteAnalysis> getNote(@RequestHeader("device-id") String deviceId, @PathVariable("id") long id) {
        return noteService.getNoteById(deviceId, id);
    }


    @Operation(
            summary = "创建笔记",
            description = "创建笔记")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {@Content(schema = @Schema(implementation = NoteAnalysis.class), mediaType = "application/json")}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema())}),
            @ApiResponse(responseCode = "500", content = {@Content(schema = @Schema())})})
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public Mono<NoteAnalysis> createNote(@RequestHeader("device-id") String deviceId, @RequestBody CreateNoteAnalysisRequest request) {
        return noteService.createNote(deviceId, request);
    }

    @Operation(
            summary = "创建图片笔记",
            description = "创建图片笔记")
    @RequestMapping(value = "/createImageNote", method = RequestMethod.POST)
    public Mono<NoteAnalysis> createImageNote(@RequestHeader("device-id") String deviceId, @RequestPart("file") FilePart file) {
        return noteService.createImageNote(deviceId, file);
    }


    @Operation(
            summary = "更新笔记",
            description = "更新笔记")
    @RequestMapping(value = "/update/{id}", method = RequestMethod.POST)
    public Mono<NoteAnalysis> updateNote(@PathVariable("id") long id, @RequestBody UpdateNoteAnalysisRequest request) {
        return noteService.updateNote(id, request);
    }

    @Operation(
            summary = "删除笔记",
            description = "软删除笔记")
    @DeleteMapping("/delete/{id}")
    public Mono<NoteAnalysis> deleteNote(@PathVariable("id") long id) {
        return noteService.deleteNote(id);
    }


    @Operation(
            summary = "追加AI笔记",
            description = "追加AI笔记到原始笔记")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {@Content(schema = @Schema(implementation = NoteAnalysis.class), mediaType = "application/json")}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema())}),
            @ApiResponse(responseCode = "500", content = {@Content(schema = @Schema())})})
    @RequestMapping(value = "/appendToOriginalText", method = RequestMethod.POST)
    public Mono<NoteAnalysis> appendToOriginalText(@RequestBody AppendToOriginalTextRequest request) {
        return noteService.appendToOriginalText(request.getId(), request.getToAppendText());
    }

}
