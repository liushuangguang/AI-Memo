//package com.newtech.note.config.Filter;
//
//import com.auth0.jwt.interfaces.DecodedJWT;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.newtech.note.entity.dto.UserInfo;
//import com.newtech.note.util.JWTUtils;
//import lombok.RequiredArgsConstructor;
//import org.apache.commons.lang3.StringUtils;
//import org.jetbrains.annotations.NotNull;
//import org.springframework.core.annotation.Order;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.stereotype.Component;
//import org.springframework.web.reactive.function.server.HandlerFilterFunction;
//import org.springframework.web.reactive.function.server.HandlerFunction;
//import org.springframework.web.reactive.function.server.ServerRequest;
//import org.springframework.web.reactive.function.server.ServerResponse;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//import javax.servlet.annotation.WebFilter;
//import java.nio.charset.StandardCharsets;
//import java.util.Base64;
//
//@Component
//@RequiredArgsConstructor
//@Order(1)
//@WebFilter(urlPatterns = "/audio/*, /note/analysis/*, /v2/note/analysis/*, /note/analysis/record/*, " +
//        "/note/assist/*, /test/note/analysis/todo ")
//public class JwtAuthenticationFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {
//
//    private final JWTUtils jwtUtils;
//
//    @NotNull
//    @Override
//    public Mono<ServerResponse> filter(ServerRequest request, @NotNull HandlerFunction<ServerResponse> next) {
//        String authorizationHeader = request.headers().header("Authorization")
//                .stream()
//                .findFirst()
//                .orElse(null);
//        ServerWebExchange exchange = request.exchange();
//        ServerHttpRequest req = exchange.getRequest();
////        ServerHttpResponse res = exchange.getResponse();
//        if (req.getURI().getPath().contains("/login")) {
//            //如果是登陆，就放行
//            return next.handle(request);
//        }
////        HttpHeaders headers = req.getHeaders();
////        String jwtToken = headers.getFirst("token");
////        if (StringUtils.isBlank(jwtToken)) {
////            return ServerResponse.status(HttpStatus.UNAUTHORIZED).body(Mono.just("Unauthorized"), String.class);
////        }
//
//        // 如果没有携带 Authorization Header，直接放行
//        if (authorizationHeader == null || !authorizationHeader.startsWith("Mobile")) {
//            return next.handle(request);
//        }
//
//        // 获取 JWT
//        String token = authorizationHeader.substring(6);  // 去掉 "Mobile" 前缀
//
//        try {
//            // 校验 JWT，获取其中的主体信息
//            DecodedJWT decodedJWT = jwtUtils.checkJwt(token);
//            String payload = decodedJWT.getPayload();
//
//            // Base64Url 解码
//            byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
//            String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
//
//            // 将解码后的字符串转换为 JSON 对象
//            ObjectMapper objectMapper = new ObjectMapper();
//            UserInfo user = objectMapper.readValue(decodedString, UserInfo.class);
//
//            // 检查 token 是否即将过期
//            int verifyResult = jwtUtils.verifyToken(decodedJWT.getClaims());
//            if (verifyResult == -1) { // token 未过期，但需要刷新
//                // 生成新的 token
//                String newToken = jwtUtils.createJwt(user);
//                // 将新的 token 返回给客户端
//                req.mutate().header("X-User-Info", token).header("X-New-Token", newToken).build();
//            } else {
//                req.mutate().header("X-User-Info", token).build();
//            }
//            // 将 body（如用户信息）传递到请求上下文
//            req.mutate().header("X-User-Info", token).build();
//            return next.handle(request);
//        } catch (Exception e) {
//            // JWT 校验失败，返回 401
//            return ServerResponse.status(HttpStatus.UNAUTHORIZED).body(Mono.just("Invalid Token"), String.class);
//        }
//    }
//}