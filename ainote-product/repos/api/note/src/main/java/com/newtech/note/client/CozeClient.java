package com.newtech.note.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.newtech.note.common.BusinessException;
import com.newtech.note.entity.dto.PointsChangeEnum;
import com.newtech.note.service.PointsService;
import com.newtech.note.service.UserService;
import com.newtech.note.security.GuestIdentityValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Client for the Coze workflow API.
 *
 * <p>Credentials are intentionally supplied by the backend environment. They
 * must never be embedded in the mobile application or committed to source.</p>
 */
@Component
@Slf4j
public class CozeClient {
    private static final long GUEST_UID = Long.MIN_VALUE;
    private static final String MOBILE_AUTH_PREFIX = "Mobile";

    private final WebClient webClient;
    private final PointsService pointsService;
    private final UserService userService;
    private final String apiKey;
    private final String workflowUrl;
    private final boolean guestEnabled;
    private final long fallbackChargeTokens;
    private final Duration requestTimeout;

    public CozeClient(WebClient webClient,
                      PointsService pointsService,
                      UserService userService,
                      String apiKey,
                      String workflowUrl,
                      boolean guestEnabled,
                      long fallbackChargeTokens) {
        this(webClient, pointsService, userService, apiKey, workflowUrl, guestEnabled,
                fallbackChargeTokens, 30);
    }

    @Autowired
    public CozeClient(WebClient webClient,
                      PointsService pointsService,
                      UserService userService,
                      @Value("${coze.api-key:}") String apiKey,
                      @Value("${coze.workflow-url:https://api.coze.cn/v1/workflow/run}") String workflowUrl,
                      @Value("${app.guest.enabled:false}") boolean guestEnabled,
                      @Value("${coze.workflow-fallback-charge-tokens:1000}") long fallbackChargeTokens,
                      @Value("${coze.timeout-seconds:30}") long timeoutSeconds) {
        this.webClient = webClient;
        this.pointsService = pointsService;
        this.userService = userService;
        this.apiKey = StringUtils.trimToEmpty(apiKey);
        this.workflowUrl = workflowUrl;
        this.guestEnabled = guestEnabled;
        this.fallbackChargeTokens = Math.max(1, fallbackChargeTokens);
        this.requestTimeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
    }

    private Map<String, Object> buildRequestBody(String workflowId,
                                                  String botId,
                                                  Map<String, Object> parameters,
                                                  Map<String, Object> ext) {
        Map<String, Object> body = new HashMap<>();
        body.put("workflow_id", workflowId);
        if (StringUtils.isNotBlank(botId)) {
            body.put("bot_id", botId);
        }
        body.put("parameters", parameters == null ? Map.of() : parameters);
        if (MapUtils.isNotEmpty(ext)) {
            body.put("ext", ext);
        }
        return body;
    }

    /** Calls a Coze workflow without logging prompt or response contents. */
    public Mono<String> callCozeWorkflowApiFiltered(String workflowId,
                                                     String botId,
                                                     Map<String, Object> parameters,
                                                     Map<String, Object> ext,
                                                     ServerHttpRequest request) {
        if (StringUtils.isBlank(workflowId)) {
            return Mono.error(new BusinessException("COZE_WORKFLOW_MISSING", "Coze workflow id is missing"));
        }
        if (StringUtils.isBlank(apiKey)) {
            return Mono.error(new BusinessException("COZE_NOT_CONFIGURED", "Coze API key is not configured"));
        }

        return Mono.defer(() -> resolveUid(request)
                .flatMap(uid -> ensureAvailablePoints(uid)
                        .then(executeWorkflow(workflowId, botId, parameters, ext))
                        .flatMap(response -> recordConsumption(uid, response.chargeTokens())
                                .thenReturn(response.data()))));
    }

    private Mono<WorkflowResult> executeWorkflow(String workflowId,
                                                 String botId,
                                                 Map<String, Object> parameters,
                                                 Map<String, Object> ext) {
        return webClient.post()
                .uri(workflowUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(buildRequestBody(workflowId, botId, parameters, ext))
                .retrieve()
                .onStatus(status -> status.isError(), response -> Mono.just(
                        new BusinessException("COZE_HTTP_" + response.statusCode().value(),
                                "Coze service request failed with HTTP " + response.statusCode().value())))
                .bodyToMono(JsonNode.class)
                .timeout(requestTimeout)
                .switchIfEmpty(Mono.error(new BusinessException("COZE_EMPTY_RESPONSE", "Coze returned an empty response")))
                .onErrorMap(this::mapProviderFailure)
                .flatMap(response -> parseWorkflowResponse(workflowId, response));
    }

    private Mono<WorkflowResult> parseWorkflowResponse(String workflowId, JsonNode response) {
        if (response == null || !response.isObject()) {
            return malformedResponse();
        }
        JsonNode codeNode = response.get("code");
        if (codeNode == null || !codeNode.isIntegralNumber()) {
            return malformedResponse();
        }
        int code = codeNode.intValue();
        if (code != 0) {
            log.warn("Coze workflow {} failed with provider code {}", workflowId, code);
            return Mono.error(new BusinessException("COZE_" + code,
                    safeProviderMessage(response.path("msg").asText(null))));
        }
        JsonNode dataNode = response.get("data");
        if (dataNode == null || dataNode.isNull()) {
            return Mono.error(new BusinessException("COZE_MISSING_DATA", "Coze workflow returned no data"));
        }
        if (!dataNode.isTextual() || StringUtils.isBlank(dataNode.textValue())) {
            return malformedResponse();
        }
        JsonNode tokenNode = response.get("token");
        long chargeTokens = tokenNode != null && tokenNode.isIntegralNumber()
                && tokenNode.canConvertToLong() && tokenNode.longValue() > 0
                ? tokenNode.longValue()
                : fallbackChargeTokens;
        log.info("Coze workflow {} completed", workflowId);
        return Mono.just(new WorkflowResult(dataNode.textValue(), chargeTokens));
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
            return "Coze workflow execution failed";
        }
        String trimmed = message.trim();
        return trimmed.length() <= 200 ? trimmed : trimmed.substring(0, 200);
    }

    private Throwable mapProviderFailure(Throwable failure) {
        if (failure instanceof BusinessException) {
            return failure;
        }
        if (failure instanceof DecodingException) {
            return new BusinessException("COZE_MALFORMED_RESPONSE", "Coze returned an unreadable response");
        }
        if (failure instanceof TimeoutException) {
            return new BusinessException("COZE_TIMEOUT", "Coze workflow request timed out");
        }
        return new BusinessException("COZE_TRANSPORT_ERROR", "Coze service could not be reached");
    }

    private <T> Mono<T> malformedResponse() {
        return Mono.error(new BusinessException("COZE_MALFORMED_RESPONSE", "Coze returned a malformed response"));
    }

    private record WorkflowResult(String data, long chargeTokens) {
    }
}
