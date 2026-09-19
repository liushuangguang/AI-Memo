package com.newtech.note.service.impl;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.newtech.note.common.BusinessException;
import com.newtech.note.entity.dto.AudioSession;
import com.newtech.note.entity.dto.NoteAnalysisHistory;
import com.newtech.note.entity.request.DeleteNoteDiscussAudioRequest;
import com.newtech.note.entity.request.GetNoteDiscussAudioRequest;
import com.newtech.note.entity.request.OrganizeToDoListNoteRequest;
import com.newtech.note.entity.request.UpdateNoteDiscussAudioRequest;
import com.newtech.note.repositories.AudioSessionRepository;
import com.newtech.note.repositories.NoteAnalysisHistoryRepository;
import com.newtech.note.repositories.NoteAnalysisRepository;
import com.newtech.note.service.AudioSessionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class AudioSessionServiceImpl implements AudioSessionService {

    private final AudioSessionRepository audioSessionRepository;
    private final NoteAnalysisRepository noteAnalysisRepository;
    private final NoteAnalysisHistoryRepository noteAnalysisHistoryRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String cozeApiKey;
    private final String cozeChatUrl;
    private final Duration requestTimeout;

    public AudioSessionServiceImpl(AudioSessionRepository audioSessionRepository,
                                   NoteAnalysisRepository noteAnalysisRepository,
                                   NoteAnalysisHistoryRepository noteAnalysisHistoryRepository, WebClient webClient,
                                   DatabaseClient databaseClient,
                                   ObjectMapper objectMapper,
                                   String cozeApiKey,
                                   String cozeChatUrl) {
        this(audioSessionRepository, noteAnalysisRepository, noteAnalysisHistoryRepository,
                webClient, databaseClient, objectMapper, cozeApiKey, cozeChatUrl, 30);
    }

    @Autowired
    public AudioSessionServiceImpl(AudioSessionRepository audioSessionRepository,
                                   NoteAnalysisRepository noteAnalysisRepository,
                                   NoteAnalysisHistoryRepository noteAnalysisHistoryRepository,
                                   WebClient webClient,
                                   DatabaseClient databaseClient,
                                   ObjectMapper objectMapper,
                                   @Value("${coze.api-key:}") String cozeApiKey,
                                   @Value("${coze.chat-url:https://api.coze.cn/open_api/v2/chat}") String cozeChatUrl,
                                   @Value("${coze.timeout-seconds:30}") long timeoutSeconds) {
        this.audioSessionRepository = audioSessionRepository;
        this.noteAnalysisRepository = noteAnalysisRepository;
        this.noteAnalysisHistoryRepository = noteAnalysisHistoryRepository;
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.cozeApiKey = StringUtils.trimToEmpty(cozeApiKey);
        this.cozeChatUrl = cozeChatUrl;
        this.requestTimeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
    }

    @Override
    public Mono<String> getOrganizeToDoListNote(OrganizeToDoListNoteRequest request) {
        String requestBody = buildRequestBody(request.getMessageContent(), "7413710844229468160", false);
        return callCozeBotApiFiltered(requestBody)
                .publishOn(Schedulers.boundedElastic())
                .doOnSuccess(success -> {
                    AudioSession audioSession = new AudioSession();
                    audioSession.setDeviceId(request.getDeviceId());
                    audioSession.setMessageContent(request.getMessageContent());
                    audioSession.setResponse(success);
                    audioSession.setCreatedAt(LocalDateTime.now());
                    audioSession.setUpdatedAt(LocalDateTime.now());
                    audioSession.setDeleted(false);
                    audioSession.setDeletedAt(null);
                    audioSessionRepository.save(audioSession).subscribe();
                });
    }

    /**
     * 构建请求体
     *
     * @param message 请求体
     * @param botId   机器人id
     * @return 请求体
     */
    private String buildRequestBody(String message, String botId, boolean stream) {
        Map<String, Object> params = new HashMap<>();
        params.put("conversation_id", "123");
        params.put("bot_id", botId);
        params.put("user", "29032201862555");
        params.put("query", message);
        params.put("stream", stream);
        String body;
        try {
            body = objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return body;
    }

    private Mono<String> callCozeBotApiFiltered(String requestBody) {
        if (StringUtils.isBlank(cozeApiKey)) {
            return Mono.error(new BusinessException("COZE_NOT_CONFIGURED", "Coze API key is not configured"));
        }
        return webClient.post()
                .uri(cozeChatUrl)
                .header("Authorization", "Bearer " + cozeApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.isError(), response -> Mono.just(
                        new BusinessException("COZE_HTTP_" + response.statusCode().value(),
                                "Coze service request failed with HTTP " + response.statusCode().value())))
                .bodyToMono(JsonNode.class)
                .timeout(requestTimeout)
                .switchIfEmpty(Mono.error(new BusinessException("COZE_EMPTY_RESPONSE", "Coze returned an empty response")))
                .onErrorMap(this::mapCozeFailure)
                .flatMap(this::parseCozeAnswer);
    }

    private Mono<String> parseCozeAnswer(JsonNode response) {
        if (response == null || !response.isObject()) {
            return Mono.error(malformedCozeResponse());
        }
        JsonNode codeNode = response.get("code");
        if (codeNode == null || (!codeNode.isTextual() && !codeNode.isIntegralNumber())) {
            return Mono.error(malformedCozeResponse());
        }
        String code = codeNode.asText();
        if (!"0".equals(code)) {
            return Mono.error(new BusinessException("COZE_" + code,
                    safeProviderMessage(response.path("msg").asText(null))));
        }
        JsonNode messagesNode = response.get("messages");
        if (messagesNode == null || !messagesNode.isArray()) {
            return Mono.error(malformedCozeResponse());
        }
        for (JsonNode messageNode : messagesNode) {
            if (messageNode != null
                    && messageNode.isObject()
                    && "answer".equals(messageNode.path("type").asText(null))) {
                JsonNode contentNode = messageNode.get("content");
                if (contentNode == null || !contentNode.isTextual()) {
                    return Mono.error(malformedCozeResponse());
                }
                if (StringUtils.isNotBlank(contentNode.textValue())) {
                    return Mono.just(contentNode.textValue());
                }
            }
        }
        return Mono.error(new BusinessException("COZE_EMPTY_RESPONSE", "Coze returned no answer"));
    }

    private Throwable mapCozeFailure(Throwable failure) {
        if (failure instanceof BusinessException) {
            return failure;
        }
        if (failure instanceof DecodingException) {
            return new BusinessException("COZE_MALFORMED_RESPONSE", "Coze returned an unreadable response");
        }
        if (failure instanceof TimeoutException) {
            return new BusinessException("COZE_TIMEOUT", "Coze audio request timed out");
        }
        return new BusinessException("COZE_TRANSPORT_ERROR", "Coze service could not be reached");
    }

    private BusinessException malformedCozeResponse() {
        return new BusinessException("COZE_MALFORMED_RESPONSE", "Coze returned a malformed response");
    }

    private String safeProviderMessage(String message) {
        if (StringUtils.isBlank(message)) {
            return "Coze bot execution failed";
        }
        String trimmed = message.trim();
        return trimmed.length() <= 200 ? trimmed : trimmed.substring(0, 200);
    }

    @Override
    public Mono<NoteAnalysisHistory> updateNoteDiscussAudio(UpdateNoteDiscussAudioRequest request) {
        Long id = request.getId();
        JsonNode talkSnapshot = request.getTalkSnapshot();
        return noteAnalysisHistoryRepository.findById(id)
                .flatMap(exists -> {
                    try {
                        String talString = talkSnapshot.toString();
                        exists.setTalkSnapshot(talString);
                    } catch (Exception e) {
                    }
                    exists.setUpdatedAt(LocalDateTime.now());
                    return noteAnalysisHistoryRepository.save(exists);
                });
    }

    @Override
    public Mono<Integer> deleteNoteDiscussAudio(DeleteNoteDiscussAudioRequest request) {
        Long id = request.getSessionId();

        return noteAnalysisHistoryRepository.deleteNoteDiscussAudio(id);
    }

    @Override
    public Mono<JsonNode> getNoteDiscussAudio(GetNoteDiscussAudioRequest request) {
        Long id = request.getSessionId();
        return noteAnalysisHistoryRepository.findById(id)
                .flatMap(exists -> {
                    ObjectMapper mapper = new ObjectMapper();
                    String talkSnapshotString = exists.getTalkSnapshot();
                    JsonNode actualObj;

                    if (StrUtil.isEmptyIfStr(talkSnapshotString))
                        return Mono.just(JsonNodeFactory.instance.objectNode());

                    try {
                        actualObj = mapper.readTree(talkSnapshotString);
                    } catch (JsonProcessingException e) {
                        log.warn("Unable to parse stored audio discussion snapshot");
                        return Mono.just(JsonNodeFactory.instance.objectNode());
                    }

                    return Mono.just(actualObj);
                });
    }

    @Override
    public Mono<String> noteDiscussAudioAiSummary(JsonNode talkContent) {
        String requestBody = buildRequestBody(talkContent.toString(), "7418482518376267830", false);
        return callCozeBotApiFiltered(requestBody);
    }
}
