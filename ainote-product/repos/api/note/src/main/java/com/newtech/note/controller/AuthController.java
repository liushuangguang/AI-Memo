package com.newtech.note.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.newtech.note.common.BusinessException;
import com.newtech.note.common.CodeTypeEnum;
import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.entity.dto.LoginRes;
import com.newtech.note.entity.dto.UserInfo;
import com.newtech.note.entity.dto.UserInfoDTO;
import com.newtech.note.entity.request.LoginReq;
import com.newtech.note.service.UserService;
import com.newtech.note.util.JWTUtils;
import com.newtech.note.util.SnowflakeIdGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * 用户注册登录
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@AllArgsConstructor
@Tag(name = "注册登录处理器", description = "用户注册、登陆相关操作")
public class AuthController {

    private static final String MOBILE_AUTHORIZATION_PREFIX = "Mobile";

    private final UserService userService;
    private final JWTUtils jwtUtils;

    @Operation(summary = "用户登录", description = "用户登录并获取令牌，获得令牌后所有ai相关的请求头Authorization中需拼接Mobile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "401", description = "凭证无效")
    })
    @PostMapping("login")
    public Mono<NoteBaseResponse<LoginRes>> login(@RequestBody LoginReq req) {
        return userService.checkExist(req.getMobile())
                .flatMap(user -> {
                    // 如果用户已存在，走登陆流程
                    Mono<NoteBaseResponse<LoginRes>> login = userService.login(req.getMobile(), req.getCode());
                    return login;
                })
                // 如果用户不存在走注册登录流程
                .switchIfEmpty(Mono.defer(() -> {
                    long newUid = SnowflakeIdGenerator.getInstance().nextId(UserInfo.class);
                    Mono<NoteBaseResponse<LoginRes>> register = userService.register(req.getMobile(), req.getCode(), newUid);

                    return register;
                }));
    }


    @Operation(summary = "发送验证码", description = "发送验证码")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "发送成功")
    })
    @PostMapping("/sendSMSCode")
    public Mono<NoteBaseResponse<Void>> sendSMSCode(@RequestParam(value = "phone") String phone,
                                                    @RequestParam(value = "type") CodeTypeEnum type,
                                                    ServerHttpRequest request) {
            return userService.sendSMSCode(phone, type, request);
    }

    @Operation(summary = "验证token是否有效", description = "请求头传入token后会校验token有效性，如果有效返回之前token，无效会返回最新token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "发送成功")
    })
    @PostMapping("/verifyToken")
    public Mono<NoteBaseResponse<String>> verifyToken(ServerHttpRequest req) {
        return Mono.defer(() -> {
            String authorizationHeader = req == null
                    ? null : req.getHeaders().getFirst("Authorization");
            if (StringUtils.isBlank(authorizationHeader)
                    || !authorizationHeader.startsWith(MOBILE_AUTHORIZATION_PREFIX)) {
                return Mono.error(unauthorized());
            }

            String token = authorizationHeader.substring(MOBILE_AUTHORIZATION_PREFIX.length());
            if (StringUtils.isBlank(token) || containsUnsafeWhitespace(token)) {
                return Mono.error(unauthorized());
            }

            final DecodedJWT decodedJWT;
            final Long claimedUid;
            final String claimedMobile;
            final int verifyResult;
            try {
                decodedJWT = jwtUtils.checkJwt(token);
                claimedUid = decodedJWT.getClaim("uid").asLong();
                claimedMobile = decodedJWT.getClaim("mobile").asString();
                verifyResult = jwtUtils.verifyToken(decodedJWT.getClaims());
            } catch (Exception failure) {
                return Mono.error(unauthorized());
            }
            if (claimedUid == null || claimedUid <= 0 || StringUtils.isBlank(claimedMobile)) {
                return Mono.error(unauthorized());
            }
            if (verifyResult != -1) {
                return userService.getTokenByUid(claimedUid)
                        .filter(token::equals)
                        .map(NoteBaseResponse::success)
                        .switchIfEmpty(Mono.error(unauthorized()));
            }

            UserInfoDTO user = new UserInfoDTO(claimedUid, claimedMobile, null);
            final String newToken;
            try {
                newToken = jwtUtils.createJwt(user);
            } catch (Exception failure) {
                return Mono.error(unauthorized());
            }
            return userService.compareAndSetToken(claimedUid, token, newToken)
                    .flatMap(updated -> updated
                            ? userService.getTokenByUid(claimedUid)
                            .map(NoteBaseResponse::success)
                            .switchIfEmpty(Mono.error(unauthorized()))
                            : Mono.error(unauthorized()));
        });
    }

    private boolean containsUnsafeWhitespace(String value) {
        return value.codePoints().anyMatch(codePoint -> Character.isWhitespace(codePoint)
                || Character.isSpaceChar(codePoint)
                || Character.isISOControl(codePoint));
    }

    private BusinessException unauthorized() {
        return new BusinessException("UNAUTHORIZED", "A valid current token is required");
    }

}
