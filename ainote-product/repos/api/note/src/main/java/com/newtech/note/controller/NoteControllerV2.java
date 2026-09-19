package com.newtech.note.controller;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.common.NotePageData;
import com.newtech.note.entity.dto.Note;
import com.newtech.note.entity.request.CreateNoteRequest;
import com.newtech.note.entity.request.SearchNoteRequest;
import com.newtech.note.entity.request.UpdateNoteRequest;
import com.newtech.note.entity.request.noteModule.*;
import com.newtech.note.service.NoteServiceV2;
import com.newtech.note.config.Filter.MyWebFilter;
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

@RestController
@RequestMapping("/v2/note")
@Tag(name = "笔记处理器", description = "Note处理器，用于创建、更新、删除、查询笔记")
public class NoteControllerV2 {


    private final NoteServiceV2 noteService;

    public NoteControllerV2(NoteServiceV2 noteService) {
        this.noteService = noteService;
    }

    @Operation(
            summary = "获得所有笔记列表",
            description = "查询当前设备下所有笔记列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {@Content(schema = @Schema(implementation = Note.class), mediaType = "application/json")}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema())}),
            @ApiResponse(responseCode = "500", content = {@Content(schema = @Schema())})})
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public Flux<Note> getNoteList(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @RequestBody SearchNoteRequest request) {
        return noteService.listNote(deviceId, request);
    }

    @Operation(
            summary = "分页查询笔记列表",
            description = "分页查询笔记列表，返回分页信息和笔记列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {@Content(schema = @Schema(implementation = Note.class), mediaType = "application/json")}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema())}),
            @ApiResponse(responseCode = "500", content = {@Content(schema = @Schema())})})
    @RequestMapping(value = "/pagination", method = RequestMethod.POST)
    public Mono<NoteBaseResponse<NotePageData<Note>>> getNotePagination(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @RequestBody SearchNoteRequest request) {
        return noteService.notePagination(deviceId, request);
    }


    @Operation(
            summary = "获得单个笔记详情",
            description = "获得单个笔记详情")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {@Content(schema = @Schema(implementation = Note.class), mediaType = "application/json")}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema())}),
            @ApiResponse(responseCode = "500", content = {@Content(schema = @Schema())})})
    @GetMapping("/{id}")
    public Mono<Note> getNote(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @PathVariable("id") String id) {
        return noteService.getNoteById(deviceId, id);
    }


    @Operation(
            summary = "创建笔记",
            description = "创建笔记，创建笔记是传入的文本内容会放入第一个text_data模块里，后续更新将更新第一个text_data模块的内容，不过note本身也派生了content字段，方便于搜索和展示")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {@Content(schema = @Schema(implementation = Note.class), mediaType = "application/json")}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema())}),
            @ApiResponse(responseCode = "500", content = {@Content(schema = @Schema())})})
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public Mono<NoteBaseResponse<Note>> createNote(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @RequestBody CreateNoteRequest request) {
        return noteService.createNote(deviceId, request);
    }

    @Operation(
            summary = "创建图片笔记",
            description = "创建图片笔记")
    @RequestMapping(value = "/createImageNote", method = RequestMethod.POST)
    public Mono<NoteBaseResponse<Note>> createImageNote(
            @RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId,
            @RequestPart("file") FilePart file,
            @RequestPart(value = "recognizedText", required = false) String recognizedText,
            @RequestPart(value = "requestId", required = false) String requestId) {
        return noteService.createImageNote(deviceId, file, recognizedText, requestId);
    }


    @Operation(
            summary = "更新笔记",
            description = "更新笔记")
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public Mono<NoteBaseResponse<Note>> updateNote(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @RequestBody UpdateNoteRequest request) {
        return noteService.updateNote(deviceId, request);
    }

    @Operation(
            summary = "删除笔记",
            description = "软删除笔记")
    @DeleteMapping("/delete/{id}")
    public Mono<NoteBaseResponse<Note>> deleteNote(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @PathVariable("id") String id) {
        return noteService.deleteNote(deviceId, id);
    }

    @Operation(
            summary = "添加笔记模块",
            description = "添加笔记模块（笔记组件）")
    @PostMapping("/module/add")
    public Mono<NoteBaseResponse<Note>> addNoteModule(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @RequestBody AddNoteModuleRequest request) {
        return noteService.addNoteModule(deviceId, request);
    }

    @Operation(
            summary = "替换正文模块",
            description = "这不是一个通用的接口，目前只有在点击一键替换按钮时调用，将正文模块的展示状态设置为hide，并添加整理后的正文模块BacklogModule")
    @PostMapping("/module/replace")
    public Mono<NoteBaseResponse<Note>> replaceTextModule(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @RequestBody ReplaceTextModuleRequest request) {
        return noteService.replaceTextModule(deviceId, request);
    }


    @Operation(
            summary = "更新笔记模块",
            description = "更新笔记模块（笔记组件）")
    @PostMapping("/module/update")
    public Mono<NoteBaseResponse<Note>> updateNoteModule(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @RequestBody UpdateNoteModuleRequest request) {
        return noteService.updateNoteModule(deviceId, request);
    }

    @Operation(
            summary = "删除笔记模块",
            description = "删除笔记模块（笔记组件）")
    @PostMapping("/module/delete")
    public Mono<NoteBaseResponse<Note>> deleteNoteModule(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @RequestBody DeleteNoteModuleRequest request) {
        return noteService.deleteNoteModule(deviceId, request);
    }

    @Operation(
            summary = "移动笔记模块",
            description = "移动笔记模块（笔记组件）")
    @PostMapping("/module/move")
    public Mono<NoteBaseResponse<Note>> moveNoteModule(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @RequestBody MoveNoteModuleRequest request) {
        return noteService.moveNoteModule(deviceId, request);
    }

    @Operation(
            summary = "添加笔记模块元素",
            description = "添加笔记模块元素（笔记组件元素）")
    @PostMapping("/module/item/add")
    public Mono<NoteBaseResponse<Note>> addNoteModuleItem(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @RequestBody AddNoteModuleItemRequest request) {
        return noteService.addNoteModuleItem(deviceId, request);
    }

    @Operation(
            summary = "更新笔记模块元素",
            description = "更新笔记模块元素（笔记组件）")
    @PostMapping("/module/item/update")
    public Mono<NoteBaseResponse<Note>> updateNoteModuleItem(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @RequestBody UpdateNoteModuleItemRequest request) {
        return noteService.updateNoteModuleItem(deviceId, request);
    }

    @Operation(
            summary = "删除笔记模块元素",
            description = "更新笔记模块元素（笔记组件）")
    @PostMapping("/module/item/delete")
    public Mono<NoteBaseResponse<Note>> deleteNoteModuleItem(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @RequestBody DeleteNoteModuleItemRequest request) {
        return noteService.deleteNoteModuleItem(deviceId, request);
    }
}
