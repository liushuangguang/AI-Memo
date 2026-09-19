//package com.newtech.note.config.Filter;
//
//import com.newtech.note.config.SecurityLocalContext;
//import com.newtech.note.entity.dto.UserAuthentication;
//import com.newtech.note.service.PointsService;
//import lombok.AllArgsConstructor;
//import org.jetbrains.annotations.NotNull;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import org.springframework.web.server.WebFilter;
//import org.springframework.web.server.WebFilterChain;
//import reactor.core.publisher.Mono;
//
//import java.util.Arrays;
//import java.util.Objects;
//import java.util.Optional;
//
///**
// * 拦截器，用于使用ai功能前的积分判断，因拦截器会让threadlocal变成null，故先注释
// */
//@Component
//@AllArgsConstructor
//public class CustomInterceptor implements WebFilter {
//
//    private final PointsService pointsService;
//    private final static String[] CONTROLLER_FILTER = {
//            "/note/analysis",
//            "/v2/note/analysis",
//            "/note/analysis",
//            "/note/assist"
//    };
//    @NotNull
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, @NotNull WebFilterChain chain) {
//        String uri = exchange.getRequest().getURI().toString();
//        if (Arrays.stream(CONTROLLER_FILTER).noneMatch(uri::contains)) {
//            return chain.filter(exchange);
//        }
//        String deviceId = Optional.ofNullable(SecurityLocalContext.getUserAuthentication())
//                .map(UserAuthentication::getDeviceId)
//                .orElseThrow(() -> new RuntimeException("Authentication is null"));
//        Mono<Double> availablePoints = pointsService.getAvailablePoints(null, deviceId);
//
//        return availablePoints.flatMap(points -> {
//            if (Objects.equals(points, 0.0)) {
//                return Mono.error(new RuntimeException("没有足够的积分，请充值后重试"));
//            } else {
//                return chain.filter(exchange);
//            }
//        });
//    }
//}
