package com.newtech.note.controller;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.common.NotePageData;
import com.newtech.note.config.Filter.MyWebFilter;
import com.newtech.note.entity.dto.NoteTheme;
import com.newtech.note.entity.request.CreateNoteThemeRequest;
import com.newtech.note.entity.request.SearchNoteThemeRequest;
import com.newtech.note.entity.request.UpdateNoteThemeRequest;
import com.newtech.note.entity.request.MergeNoteThemeRequest;
import com.newtech.note.entity.vo.NoteThemeCandidate;
import com.newtech.note.entity.vo.NoteThemeMergeResult;
import com.newtech.note.service.NoteThemeService;
import com.newtech.note.service.NoteThemeMergeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/note/theme")
@Tag(name = "笔记主题处理器", description = "Note主题处理器，用于创建、更新、删除、查询笔记主题处理器")
public class NoteThemeController {
    private final NoteThemeService noteThemeService;
    private final NoteThemeMergeService noteThemeMergeService;

    public NoteThemeController(NoteThemeService noteThemeService,
                               NoteThemeMergeService noteThemeMergeService) {
        this.noteThemeService = noteThemeService;
        this.noteThemeMergeService = noteThemeMergeService;
    }

    @Operation(
            summary = "创建笔记主题",
            description = "创建笔记主题，创建笔记主题需要提供主题名称、描述等信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {@Content(schema = @Schema(implementation = NoteTheme.class), mediaType = "application/json")}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema())}),
            @ApiResponse(responseCode = "500", content = {@Content(schema = @Schema())})})
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public Mono<NoteBaseResponse<NoteTheme>> createNoteTheme(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @RequestBody CreateNoteThemeRequest request) {
        return noteThemeService.createNoteTheme(deviceId, request);
    }


    @Operation(
            summary = "更新笔记主题",
            description = "更新笔记主题")
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public Mono<NoteBaseResponse<NoteTheme>> updateNoteTheme(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @RequestBody UpdateNoteThemeRequest request) {
        return noteThemeService.updateNoteTheme(deviceId, request);
    }

    @Operation(
            summary = "分页查询笔记主题列表",
            description = "分页查询笔记主题列表，返回分页信息和笔记主题列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {@Content(schema = @Schema(implementation = NoteTheme.class), mediaType = "application/json")}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema())}),
            @ApiResponse(responseCode = "500", content = {@Content(schema = @Schema())})})
    @RequestMapping(value = "/pagination", method = RequestMethod.POST)
    public Mono<NoteBaseResponse<NotePageData<NoteTheme>>> getNoteThemePagination(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @RequestBody SearchNoteThemeRequest request) {
        return noteThemeService.noteThemePagination(deviceId, request);
    }

    @Operation(
            summary = "删除笔记",
            description = "软删除笔记")
    @DeleteMapping("/delete/{id}")
    public Mono<NoteBaseResponse<NoteTheme>> deleteNoteTheme(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @PathVariable("id") String id) {
        return noteThemeService.deleteNoteTheme(deviceId, id);
    }

    @PostMapping("/{id}/candidates")
    public Mono<NoteBaseResponse<List<NoteThemeCandidate>>> candidates(
            @RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId,
            @PathVariable("id") String id) {
        return noteThemeMergeService.candidates(deviceId, id);
    }

    @PostMapping("/{id}/merge")
    public Mono<NoteBaseResponse<NoteThemeMergeResult>> merge(
            @RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId,
            @PathVariable("id") String id,
            @RequestBody MergeNoteThemeRequest request) {
        return noteThemeMergeService.merge(deviceId, id, request);
    }
}
