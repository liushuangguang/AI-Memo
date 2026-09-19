package com.newtech.note.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.newtech.note.common.BusinessException;
import com.newtech.note.entity.dto.PointsChangeEnum;
import com.newtech.note.service.PointsService;
import com.newtech.note.service.UserService;
import com.newtech.note.security.GuestIdentityValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/** Client for the legacy Coze bot chat endpoint still used by older features. */
@Slf4j
@Component
public class CozeClientV2 {
    private static final long GUEST_UID = Long.MIN_VALUE;
    private static final String MOBILE_AUTH_PREFIX = "Mobile";

    private final WebClient webClient;
    private final PointsService pointsService;
    private final UserService userService;
    private final String apiKey;
    private final String chatUrl;
    private final boolean guestEnabled;
    private final long fixedChargeTokens;
    private final Duration requestTimeout;

    public CozeClientV2(WebClient webClient,
                        PointsService pointsService,
                        UserService userService,
                        @Value("${coze.api-key:}") String apiKey,
                        @Value("${coze.chat-url:https://api.coze.cn/open_api/v2/chat}") String chatUrl,
                        @Value("${app.guest.enabled:false}") boolean guestEnabled,
                        @Value("${coze.legacy-fixed-charge-tokens:1000}") long fixedChargeTokens) {
        this(webClient, pointsService, userService, apiKey, chatUrl, guestEnabled,
                fixedChargeTokens, 30);
    }

    @Autowired
    public CozeClientV2(WebClient webClient,
                        PointsService pointsService,
                        UserService userService,
                        @Value("${coze.api-key:}") String apiKey,
                        @Value("${coze.chat-url:https://api.coze.cn/open_api/v2/chat}") String chatUrl,
                        @Value("${app.guest.enabled:false}") boolean guestEnabled,
                        @Value("${coze.legacy-fixed-charge-tokens:1000}") long fixedChargeTokens,
                        @Value("${coze.timeout-seconds:30}") long timeoutSeconds) {
        this.webClient = webClient;
        this.pointsService = pointsService;
        this.userService = userService;
        this.apiKey = StringUtils.trimToEmpty(apiKey);
        this.chatUrl = chatUrl;
        this.guestEnabled = guestEnabled;
        this.fixedChargeTokens = Math.max(1, fixedChargeTokens);
        this.requestTimeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
    }

    private Map<String, Object> buildRequestBody(String deviceId, String message, String botId, boolean stream) {
        Map<String, Object> body = new HashMap<>();
        body.put("bot_id", botId);
        body.put("user", StringUtils.defaultIfBlank(deviceId, "anonymous-device"));
        body.put("query", StringUtils.defaultString(message));
        body.put("stream", stream);
        return body;
    }

    public Flux<String> callStreamCozeBotApiFiltered(String deviceId,
                                                      String message,
                                                      String botId,
                                                      ServerHttpRequest request) {
        if (StringUtils.isBlank(apiKey)) {
            return Flux.error(new BusinessException("COZE_NOT_CONFIGURED", "Coze API key is not configured"));
        }
        if (StringUtils.isBlank(botId)) {
            return Flux.error(new BusinessException("COZE_BOT_MISSING", "Coze bot id is missing"));
        }

        return Flux.defer(() -> resolveUid(request)
                .flatMapMany(uid -> ensureAvailablePoints(uid)
                        .thenMany(executeStreaming(deviceId, message, botId))
                        .switchOnFirst((first, answers) -> first.hasValue()
                                ? Mono.defer(() -> recordConsumption(uid, fixedChargeTokens)).thenMany(answers)
                                : answers)));
    }

    public Mono<String> callCozeBotApiFiltered(String deviceId,
                                               String message,
                                               String botId,
                                               ServerHttpRequest request) {
        if (StringUtils.isBlank(apiKey)) {
            return Mono.error(new BusinessException("COZE_NOT_CONFIGURED", "Coze API key is not configured"));
        }
        if (StringUtils.isBlank(botId)) {
            return Mono.error(new BusinessException("COZE_BOT_MISSING", "Coze bot id is missing"));
        }

        return Mono.defer(() -> resolveUid(request)
                .flatMap(uid -> ensureAvailablePoints(uid)
                        .then(executeBlocking(deviceId, message, botId))
                        .flatMap(answer -> recordConsumption(uid, fixedChargeTokens).thenReturn(answer))));
    }

    private Flux<String> executeStreaming(String deviceId, String message, String botId) {
        return webClient.post()
                .uri(chatUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(buildRequestBody(deviceId, message, botId, true))
                .retrieve()
                .onStatus(status -> status.isError(), response -> Mono.just(
                        new BusinessException("COZE_HTTP_" + response.statusCode().value(),
                                "Coze service request failed with HTTP " + response.statusCode().value())))
                .bodyToFlux(JsonNode.class)
                .timeout(requestTimeout)
                .onErrorMap(this::mapProviderFailure)
                .<String>handle((event, sink) -> handleStreamEvent(botId, event, sink))
                .switchIfEmpty(Flux.error(new BusinessException("COZE_EMPTY_RESPONSE", "Coze stream returned no answer")));
    }

    private Mono<String> executeBlocking(String deviceId, String message, String botId) {
        return webClient.post()
                .uri(chatUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(buildRequestBody(deviceId, message, botId, false))
                .retrieve()
                .onStatus(status -> status.isError(), response -> Mono.just(
                        new BusinessException("COZE_HTTP_" + response.statusCode().value(),
                                "Coze service request failed with HTTP " + response.statusCode().value())))
                .bodyToMono(JsonNode.class)
                .timeout(requestTimeout)
                .switchIfEmpty(Mono.error(new BusinessException("COZE_EMPTY_RESPONSE", "Coze returned an empty response")))
                .onErrorMap(this::mapProviderFailure)
                .flatMap(response -> parseBlockingResponse(botId, response));
    }

    private void handleStreamEvent(String botId, JsonNode event, reactor.core.publisher.SynchronousSink<String> sink) {
        if (event == null || !event.isObject()) {
            sink.error(malformedResponse());
            return;
        }
        JsonNode codeNode = event.get("code");
        if (codeNode != null && (codeNode.isTextual() || codeNode.isIntegralNumber())
                && !"0".equals(codeNode.asText())) {
            String code = codeNode.asText();
            log.warn("Coze bot {} failed with provider code {}", botId, code);
            sink.error(new BusinessException("COZE_" + code,
                    safeProviderMessage(event.path("msg").asText(null))));
            return;
        }
        if (!event.path("event").isTextual()) {
            sink.error(malformedResponse());
            return;
        }
        String eventType = event.path("event").textValue();
        if ("error".equalsIgnoreCase(eventType) || eventType.toLowerCase().contains("error")) {
            log.warn("Coze bot {} returned a stream error event", botId);
            sink.error(new BusinessException("COZE_STREAM_ERROR", safeStreamMessage(event)));
            return;
        }
        if (!"message".equals(eventType)) {
            return;
        }
        JsonNode messageNode = event.get("message");
        if (messageNode == null || !messageNode.isObject() || !messageNode.path("type").isTextual()) {
            sink.error(malformedResponse());
            return;
        }
        String messageType = messageNode.path("type").textValue();
        if ("error".equalsIgnoreCase(messageType)) {
            sink.error(new BusinessException("COZE_STREAM_ERROR",
                    safeProviderMessage(messageNode.path("content").asText(null))));
            return;
        }
        if (!"answer".equals(messageType)) {
            return;
        }
        JsonNode contentNode = messageNode.get("content");
        if (contentNode == null || !contentNode.isTextual()) {
            sink.error(malformedResponse());
            return;
        }
        if (StringUtils.isNotBlank(contentNode.textValue())) {
            sink.next(contentNode.textValue());
        }
    }

    private Mono<String> parseBlockingResponse(String botId, JsonNode response) {
        if (response == null || !response.isObject()) {
            return Mono.error(malformedResponse());
        }
        JsonNode codeNode = response.get("code");
        if (codeNode == null || (!codeNode.isTextual() && !codeNode.isIntegralNumber())) {
            return Mono.error(malformedResponse());
        }
        String code = codeNode.asText();
        if (!"0".equals(code)) {
            log.warn("Coze bot {} failed with provider code {}", botId, code);
            return Mono.error(new BusinessException("COZE_" + code,
                    safeProviderMessage(response.path("msg").asText(null))));
        }
        JsonNode messagesNode = response.get("messages");
        if (messagesNode == null || !messagesNode.isArray()) {
            return Mono.error(malformedResponse());
        }
        for (JsonNode item : messagesNode) {
            if (item != null && item.isObject() && "answer".equals(item.path("type").asText(null))) {
                JsonNode contentNode = item.get("content");
                if (contentNode == null || !contentNode.isTextual()) {
                    return Mono.error(malformedResponse());
                }
                if (StringUtils.isNotBlank(contentNode.textValue())) {
                    return Mono.just(contentNode.textValue());
                }
            }
        }
        return Mono.error(new BusinessException("COZE_EMPTY_RESPONSE", "Coze returned no answer"));
    }

    private Mono<Long> resolveUid(ServerHttpRequest request) {
        String authorization = request == null ? null : request.getHeaders().getFirst("Authorization");
        if (GuestIdentityValidator.isGuestAuthorization(authorization)) {
            try {
                GuestIdentityValidator.requireValidGuest(request, guestEnabled);
                return Mono.just(GUEST_UID);
            } catch (BusinessException failure) {
                return Mono.error(failure);
            }
        }
        if (authorization == null || !authorization.matches("^Mobile[^\\s]+$")) {
            return Mono.error(new BusinessException("UNAUTHORIZED", "A valid mobile or guest authorization is required"));
        }
        return userService.getUidByToken(authorization.substring(MOBILE_AUTH_PREFIX.length()))
                .switchIfEmpty(Mono.error(new BusinessException("UNAUTHORIZED", "The mobile authorization is invalid")));
    }

    private Mono<Void> ensureAvailablePoints(long uid) {
        if (uid == GUEST_UID) {
            return Mono.empty();
        }
        return pointsService.getAvailablePoints(uid, null)
                .switchIfEmpty(Mono.error(new BusinessException("POINTS_UNAVAILABLE", "Unable to read available points")))
                .flatMap(points -> points == null || points <= 0
                        ? Mono.error(new BusinessException("3333", "Available points is 0"))
                        : Mono.empty());
    }

    private Mono<Void> recordConsumption(long uid, long tokens) {
        if (uid == GUEST_UID) {
            return Mono.empty();
        }
        return pointsService.updatePoints(uid, null, PointsChangeEnum.CONSUMER, null, tokens, null)
                .then();
    }

    private String safeProviderMessage(String message) {
        if (StringUtils.isBlank(message)) {
            return "Coze bot execution failed";
        }
        String trimmed = message.trim();
        return trimmed.length() <= 200 ? trimmed : trimmed.substring(0, 200);
    }

    private String safeStreamMessage(JsonNode event) {
        JsonNode messageNode = event.get("message");
        String message = messageNode != null && messageNode.isTextual()
                ? messageNode.textValue()
                : event.path("msg").asText(null);
        return safeProviderMessage(message);
    }

    private Throwable mapProviderFailure(Throwable failure) {
        if (failure instanceof BusinessException) {
            return failure;
        }
        if (failure instanceof DecodingException) {
            return new BusinessException("COZE_MALFORMED_RESPONSE", "Coze returned an unreadable response");
        }
        if (failure instanceof TimeoutException) {
            return new BusinessException("COZE_TIMEOUT", "Coze bot request timed out");
        }
        return new BusinessException("COZE_TRANSPORT_ERROR", "Coze service could not be reached");
    }

    private BusinessException malformedResponse() {
        return new BusinessException("COZE_MALFORMED_RESPONSE", "Coze returned a malformed response");
    }
}
