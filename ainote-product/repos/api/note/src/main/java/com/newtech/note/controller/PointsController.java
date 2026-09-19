package com.newtech.note.controller;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.common.UserLevelEnum;
import com.newtech.note.entity.dto.*;
import com.newtech.note.security.RequestIdentityService;
import com.newtech.note.service.PointsService;
import com.newtech.note.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 积分controller
 */
@RestController
@RequestMapping("/points")
@Tag(name = "积分处理器", description = "积分处理器，用于充值、查询积分")
public class PointsController {

    private static final double GUEST_DEBUG_POINTS = 1_000_000.0;

    private final PointsService pointsService;
    private final UserService userService;
    private final RequestIdentityService identityService;

    public PointsController(PointsService pointsService,
                            UserService userService,
                            RequestIdentityService identityService) {
        this.pointsService = pointsService;
        this.userService = userService;
        this.identityService = identityService;
    }

    @Operation(
            summary = "充值积分",
            description = "充值积分")
    @ApiResponses({
            @ApiResponse(responseCode = "410", description = "Client-managed points updates are disabled",
                    content = {@Content(schema = @Schema(implementation = NoteBaseResponse.class), mediaType = "application/json")})})
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public Mono<ResponseEntity<NoteBaseResponse<Void>>> updatePoints(@RequestBody UpdatePointsReq req,
                                                                      ServerHttpRequest request) {
        return Mono.just(ResponseEntity.status(HttpStatus.GONE)
                .body(NoteBaseResponse.failure(HttpStatus.GONE.value(),
                        "Client-managed points updates are disabled")));
    }

    @Operation(
            summary = "获取用户详细信息",
            description = "获取用户详细信息，包括用户等级，可用积分，积分历史记录（添加、消耗）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {@Content(schema = @Schema(implementation = Note.class), mediaType = "application/json")}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema())}),
            @ApiResponse(responseCode = "500", content = {@Content(schema = @Schema())})})
    @RequestMapping(value = "/get", method = RequestMethod.GET)
    public Mono<UserInfoDetail> getAvailablePoints(ServerHttpRequest req) {
        return identityService.resolve(req).flatMap(identity -> {
            if (identity.guest()) {
                return Mono.just(new UserInfoDetail(GUEST_DEBUG_POINTS, List.of(), UserLevelEnum.HUMAN));
            }
            Long uid = identity.uid();
            return pointsService.getAvailablePoints(uid, null)
                    .flatMap(availablePoints -> pointsService.getPointsUpdateHistory(uid)
                            .flatMap(pointsHistory -> userService.getUserLevel(uid)
                                    .map(user -> new UserInfoDetail(
                                            availablePoints, pointsHistory, user.getLevel()))));
        });
    }


    @Operation(
            summary = "初始化设备id",
            description = "初始化设备id，没有用户系统时临时使用")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {@Content(schema = @Schema(implementation = Note.class), mediaType = "application/json")}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema())}),
            @ApiResponse(responseCode = "500", content = {@Content(schema = @Schema())})})
    @GetMapping("/set_device_id/{deviceId}")
    public Mono<Void> getAvailablePoints(@PathVariable("deviceId") String deviceId) {
        return Mono.error(new ResponseStatusException(HttpStatus.GONE,
                "Thread-local device identity initialization has been disabled"));
    }
}
