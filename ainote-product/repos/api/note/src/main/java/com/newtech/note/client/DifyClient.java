package com.newtech.note.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.newtech.note.client.entity.dify.DifyStreamRes;
import com.newtech.note.common.BusinessException;
import com.newtech.note.security.GuestIdentityValidator;
import com.newtech.note.service.PointsService;
import com.newtech.note.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Minimal client for Dify workflow applications.
 *
 * <p>Dify application API keys are deliberately supplied by the caller. This
 * client never reads a built-in key and never logs request or response bodies.</p>
 */
@Component
@Slf4j
public class DifyClient {
    private static final String MOBILE_AUTH_PREFIX = "Mobile";

    private final WebClient webClient;
    private final PointsService pointsService;
    private final UserService userService;
    private final String baseUrl;
    private final Duration requestTimeout;
    private final boolean guestEnabled;
    @Value("${ai.chat.direct:false}")
    private boolean preferDirect;

    public DifyClient(WebClient webClient,
                      PointsService pointsService,
                      UserService userService,
                      @Value("${dify.base-url:https://api.dify.ai}") String baseUrl,
                      @Value("${dify.timeout-seconds:90}") long timeoutSeconds,
                      @Value("${app.guest.enabled:false}") boolean guestEnabled) {
        this.webClient = webClient;
        this.pointsService = pointsService;
        this.userService = userService;
        this.baseUrl = removeTrailingSlash(baseUrl);
        this.requestTimeout = Duration.ofSeconds(timeoutSeconds);
        this.guestEnabled = guestEnabled;
    }

    public Mono<String> callDifyBlockingWorkflowApiFiltered(String apiKey,
                                                             Map<String, Object> parameters,
                                                             String outputField,
                                                             ServerHttpRequest request) {
        return Mono.defer(() -> validateUsage(request)
                .flatMap(usage -> invokeBlocking(apiKey, parameters, outputField, usage)));
    }

    public Mono<String> callDifyBlockingWorkflowApiWithFallback(
            String apiKey,
            Map<String, Object> parameters,
            String outputField,
            ServerHttpRequest request,
            Supplier<Mono<DeepSeekCompletion>> fallback) {
        return Mono.defer(() -> validateUsage(request)
                .flatMap(usage -> preferDirect ? invokeDirect(usage, fallback) : invokeBlocking(apiKey, parameters, outputField, usage)
                        .onErrorResume(DifyProviderException.class, failure -> {
                            log.warn("Dify unavailable; using configured fallback");
                            return invokeDirect(usage, fallback);
                        })));
    }

    public Flux<String> callDifyStreamingWorkflowApiFiltered(String apiKey,
                                                              Map<String, Object> parameters,
                                                              ServerHttpRequest request) {
        return Flux.defer(() -> validateUsage(request)
                .flatMapMany(usage -> invokeStreaming(apiKey, parameters, usage, false)));
    }

    public Flux<String> callDifyStreamingWorkflowApiWithFallback(
            String apiKey,
            Map<String, Object> parameters,
            ServerHttpRequest request,
            Supplier<Mono<DeepSeekCompletion>> fallback) {
        return Flux.defer(() -> validateUsage(request)
                .flatMapMany(usage -> preferDirect ? invokeDirect(usage, fallback).flux() : invokeStreaming(apiKey, parameters, usage, true)
                        .onErrorResume(DifyProviderException.class, failure -> {
                            log.warn("Dify unavailable; using configured fallback");
                            return invokeDirect(usage, fallback).flux();
                        })));
    }

    private Mono<String> invokeDirect(UsageContext usage, Supplier<Mono<DeepSeekCompletion>> provider) {
        return Mono.defer(provider).flatMap(completion -> recordUsage(
                usage, TokenUsage.valid(completion.totalTokens())).thenReturn(completion.content()));
    }

    private Mono<String> invokeBlocking(String apiKey,
                                        Map<String, Object> parameters,
                                        String outputField,
                                        UsageContext usage) {
        if (StringUtils.isBlank(apiKey)) {
            return Mono.error(DifyProviderException.notConfigured());
        }
        return executeBlocking(apiKey, parameters, outputField)
                .flatMap(result -> recordBlockingUsage(usage, result));
    }

    private Flux<String> invokeStreaming(String apiKey,
                                         Map<String, Object> parameters,
                                         UsageContext usage,
                                         boolean bufferForFallback) {
        if (StringUtils.isBlank(apiKey)) {
            return Flux.error(DifyProviderException.notConfigured());
        }
        Flux<DifyStreamRes> events = executeStreamingEvents(apiKey, parameters);
        if (usage.uid() == null && !bufferForFallback) {
            StreamingResult result = new StreamingResult();
            return events.<String>handle((event, sink) -> {
                        result.accept(event);
                        if (isTextChunk(event)) {
                            sink.next(event.data().text());
                        }
                    })
                    .concatWith(Mono.defer(() -> validateStreamingCompletion(result).then(Mono.empty())));
        }
        return events.collect(StreamingResult::new, StreamingResult::accept)
                .flatMapMany(result -> validateStreamingCompletion(result)
                        .flatMap(tokenUsage -> recordStreamingUsage(usage, tokenUsage))
                        .thenMany(Flux.fromIterable(result.chunks())));
    }

    private Mono<BlockingResult> executeBlocking(String apiKey,
                                                  Map<String, Object> parameters,
                                                  String outputField) {
        return webClient.post()
                .uri(baseUrl + "/v1/workflows/run")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildRequestBody(parameters, "blocking"))
                .exchangeToMono(response -> {
                    if (response.statusCode().isError()) {
                        int status = response.statusCode().value();
                        return response.releaseBody()
                                .then(Mono.error(DifyProviderException.httpFailure(status)));
                    }
                    return response.bodyToMono(JsonNode.class);
                })
                .timeout(requestTimeout)
                .onErrorMap(this::mapProviderFailure)
                .flatMap(response -> extractBlockingOutput(response, outputField));
    }

    private Flux<DifyStreamRes> executeStreamingEvents(String apiKey,
                                                       Map<String, Object> parameters) {
        return webClient.post()
                .uri(baseUrl + "/v1/workflows/run")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildRequestBody(parameters, "streaming"))
                .exchangeToFlux(response -> {
                    if (response.statusCode().isError()) {
                        int status = response.statusCode().value();
                        return response.releaseBody()
                                .thenMany(Flux.error(DifyProviderException.httpFailure(status)));
                    }
                    return response.bodyToFlux(DifyStreamRes.class);
                })
                .timeout(requestTimeout)
                .onErrorMap(this::mapProviderFailure);
    }

    private Map<String, Object> buildRequestBody(Map<String, Object> parameters, String responseMode) {
        Map<String, Object> body = new HashMap<>();
        body.put("inputs", parameters == null ? Map.of() : parameters);
        body.put("response_mode", responseMode);
        body.put("user", UUID.randomUUID().toString());
        return body;
    }

    private Mono<BlockingResult> extractBlockingOutput(JsonNode response, String outputField) {
        JsonNode data = response == null ? null : response.get("data");
        if (data == null || !"succeeded".equals(data.path("status").asText())) {
            return Mono.error(DifyProviderException.workflowFailure());
        }
        JsonNode outputs = data.get("outputs");
        JsonNode output = outputs == null || outputField == null ? null : outputs.get(outputField);
        if (output == null || !output.isTextual()) {
            return Mono.error(DifyProviderException.missingOutput(outputField));
        }
        return Mono.just(new BlockingResult(
                AiResponseSanitizer.stripMarkdownFence(output.textValue()),
                parseTokenUsage(data.get("total_tokens"))));
    }

    private TokenUsage parseTokenUsage(JsonNode tokenNode) {
        if (tokenNode == null || tokenNode.isNull()) {
            return TokenUsage.missing();
        }
        if (!tokenNode.isIntegralNumber() || !tokenNode.canConvertToLong()) {
            return TokenUsage.invalid();
        }
        long value = tokenNode.longValue();
        return value > 0 ? TokenUsage.valid(value) : TokenUsage.invalid();
    }

    private Mono<TokenUsage> validateStreamingCompletion(StreamingResult result) {
        if (result.terminalCount() != 1 || !"succeeded".equals(result.terminalStatus())) {
            return Mono.error(DifyProviderException.workflowFailure());
        }
        return Mono.just(parseTokenUsage(result.terminalTotalTokens()));
    }

    private boolean isTextChunk(DifyStreamRes event) {
        return event != null && "text_chunk".equals(event.event())
                && event.data() != null && StringUtils.isNotBlank(event.data().text());
    }

    /**
     * Mobile requests preserve the existing points check. Guest/debug requests
     * intentionally do not touch user or points persistence.
     */
    private Mono<UsageContext> validateUsage(ServerHttpRequest request) {
        String authorization = Optional.ofNullable(request)
                .map(ServerHttpRequest::getHeaders)
                .map(headers -> headers.getFirst("Authorization"))
                .orElse("");
        if (GuestIdentityValidator.isGuestAuthorization(authorization)) {
            try {
                GuestIdentityValidator.requireValidGuest(request, guestEnabled);
                return Mono.just(UsageContext.guest());
            } catch (BusinessException failure) {
                return Mono.error(unauthorized());
            }
        }
        if (!authorization.startsWith(MOBILE_AUTH_PREFIX)) {
            return Mono.error(unauthorized());
        }
        String token = authorization.substring(MOBILE_AUTH_PREFIX.length()).trim();
        if (StringUtils.isBlank(token)) {
            return Mono.error(unauthorized());
        }
        return userService.getUidByToken(token)
                .flatMap(uid -> pointsService.getAvailablePoints(uid, null)
                        .flatMap(points -> points == null || points <= 0
                                ? Mono.error(new BusinessException("POINTS_ZERO", "Available points is 0"))
                                : Mono.just(UsageContext.mobile(uid)))
                        .switchIfEmpty(Mono.error(
                                new BusinessException("POINTS_UNAVAILABLE", "Available points could not be determined"))))
                .switchIfEmpty(Mono.error(unauthorized()));
    }

    private Mono<String> recordBlockingUsage(UsageContext usage, BlockingResult result) {
        return recordUsage(usage, result.tokenUsage()).thenReturn(result.output());
    }

    private Mono<Void> recordStreamingUsage(UsageContext usage, TokenUsage tokenUsage) {
        return recordUsage(usage, tokenUsage)
                .doOnError(error -> log.error(
                        "Unable to persist Dify token usage (failureType={})",
                        error.getClass().getSimpleName()));
    }

    private Mono<Void> recordUsage(UsageContext usage, TokenUsage tokenUsage) {
        if (usage.uid() == null) {
            return Mono.empty();
        }
        if (tokenUsage == null || tokenUsage.malformed()
                || tokenUsage.tokenCount() == null || tokenUsage.tokenCount() <= 0) {
            return Mono.error(new BusinessException(
                    "DIFY_USAGE_INVALID", "Dify response is missing valid token usage"));
        }
        return pointsService.updatePoints(usage.uid(), null,
                        com.newtech.note.entity.dto.PointsChangeEnum.CONSUMER,
                        null, tokenUsage.tokenCount(), null)
                .then();
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Valid Mobile or enabled Guest authorization required");
    }

    private Throwable mapProviderFailure(Throwable failure) {
        if (failure instanceof DifyProviderException || failure instanceof BusinessException) {
            return failure;
        }
        return DifyProviderException.transportFailure(failure);
    }

    private static String removeTrailingSlash(String value) {
        String normalized = StringUtils.defaultIfBlank(value, "https://api.dify.ai").trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private record UsageContext(Long uid) {
        static UsageContext guest() {
            return new UsageContext(null);
        }

        static UsageContext mobile(Long uid) {
            return new UsageContext(uid);
        }
    }

    private record BlockingResult(String output, TokenUsage tokenUsage) {
    }

    private record TokenUsage(Long tokenCount, boolean malformed) {
        static TokenUsage valid(long tokenCount) {
            return new TokenUsage(tokenCount, false);
        }

        static TokenUsage missing() {
            return new TokenUsage(null, false);
        }

        static TokenUsage invalid() {
            return new TokenUsage(null, true);
        }
    }

    private static final class StreamingResult {
        private final List<String> chunks = new ArrayList<>();
        private int terminalCount;
        private String terminalStatus;
        private JsonNode terminalTotalTokens;

        void accept(DifyStreamRes event) {
            if (event == null || event.data() == null) {
                return;
            }
            if ("workflow_finished".equals(event.event())) {
                terminalCount++;
                terminalStatus = event.data().status();
                terminalTotalTokens = event.data().total_tokens();
            }
            if ("text_chunk".equals(event.event())
                    && StringUtils.isNotBlank(event.data().text())) {
                chunks.add(event.data().text());
            }
        }

        List<String> chunks() {
            return chunks;
        }

        int terminalCount() {
            return terminalCount;
        }

        String terminalStatus() {
            return terminalStatus;
        }

        JsonNode terminalTotalTokens() {
            return terminalTotalTokens;
        }
    }
}
