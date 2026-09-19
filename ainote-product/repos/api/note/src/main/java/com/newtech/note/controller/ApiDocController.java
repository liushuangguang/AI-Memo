package com.newtech.note.controller;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.net.URI;

@Hidden
@RestController
@RequestMapping("/v3")
public class ApiDocController {

    private final WebClient webClient;
    @Value("${server.port}")
    private String basePort;

    public ApiDocController() throws SSLException {
        // 创建不安全的 SSL 上下文，忽略所有证书验证
        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
        // 创建 WebClient 并配置忽略证书验证
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .secure(t -> t.sslContext(sslContext)) // 忽略证书验证
                ))
                .baseUrl("localhost:" + basePort) // 假设服务运行在本地8080端口
                .build();
    }

    @GetMapping("/api-docs/**")
    public Mono<Void> handleV3ApiDocs(ServerWebExchange exchange) {
        URI uri = exchange.getRequest().getURI();

        String path = exchange.getRequest().getURI().getPath();
        String newPath = path.replaceFirst("^/v3/api-docs", "/v1/note-docs");


        // 构建新的请求 URL
        String newUrl = uri.getScheme() + "://" + uri.getAuthority() + newPath; // 假设服务运行在本地8080端口

        // 使用 WebClient 发起请求
        return webClient.get()
                .uri(newUrl)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(body -> {
                    // 设置响应状态码和响应体
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(exchange.getResponse().getStatusCode());
                    return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
                });
    }
}