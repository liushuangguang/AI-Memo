package com.newtech.note.controller;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.common.NotePageData;
import com.newtech.note.config.Filter.MyWebFilter;
import com.newtech.note.entity.dto.Backlog;
import com.newtech.note.entity.request.CreateBacklogRequest;
import com.newtech.note.entity.request.SearchBacklogRequest;
import com.newtech.note.entity.request.UpdateBacklogRequest;
import com.newtech.note.service.BacklogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/backlog")
@Tag(name = "待办处理器", description = "待办处理器，用于创建、更新、删除、查询笔记")
public class BacklogController {
    private final BacklogService backlogService;

    public BacklogController(BacklogService backlogService) {
        this.backlogService = backlogService;
    }

    @Operation(
            summary = "获得所有待办列表",
            description = "查询当前设备下所有待办列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {@Content(schema = @Schema(implementation = Backlog.class), mediaType = "application/json")}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema())}),
            @ApiResponse(responseCode = "500", content = {@Content(schema = @Schema())})})
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public Flux<Backlog> getBacklogList(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @RequestBody SearchBacklogRequest request) {
        return backlogService.listBacklog(deviceId, request);
    }

    @Operation(
            summary = "分页查询待办列表",
            description = "分页查询待办列表，返回分页信息和待办列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {@Content(schema = @Schema(implementation = Backlog.class), mediaType = "application/json")}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema())}),
            @ApiResponse(responseCode = "500", content = {@Content(schema = @Schema())})})
    @RequestMapping(value = "/pagination", method = RequestMethod.POST)
    public Mono<NoteBaseResponse<NotePageData<Backlog>>> getBacklogPagination(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @RequestBody SearchBacklogRequest request) {
        return backlogService.backlogPagination(deviceId, request);
    }


    @Operation(
            summary = "获得单个待办详情",
            description = "获得单个待办详情")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {@Content(schema = @Schema(implementation = Backlog.class), mediaType = "application/json")}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema())}),
            @ApiResponse(responseCode = "500", content = {@Content(schema = @Schema())})})
    @GetMapping("/{id}")
    public Mono<Backlog> getBacklog(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @PathVariable("id") String id) {
        return backlogService.getBacklogById(deviceId, id);
    }


    @Operation(
            summary = "创建待办",
            description = "创建待办")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {@Content(schema = @Schema(implementation = Backlog.class), mediaType = "application/json")}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema())}),
            @ApiResponse(responseCode = "500", content = {@Content(schema = @Schema())})})
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public Mono<NoteBaseResponse<Backlog>> createBacklog(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @RequestBody CreateBacklogRequest request) {
        return backlogService.createBacklog(deviceId, request);
    }


    @Operation(
            summary = "更新待办",
            description = "更新待办")
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public Mono<NoteBaseResponse<Backlog>> updateBacklog(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @RequestBody UpdateBacklogRequest request) {
        return backlogService.updateBacklog(deviceId, request);
    }

    @Operation(
            summary = "删除待办",
            description = "软删除待办")
    @DeleteMapping("/delete/{id}")
    public Mono<NoteBaseResponse<Backlog>> deleteBacklog(@RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String deviceId, @PathVariable("id") String id) {
        return backlogService.deleteBacklog(deviceId, id);
    }
}
