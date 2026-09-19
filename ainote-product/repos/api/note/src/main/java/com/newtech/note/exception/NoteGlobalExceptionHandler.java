package com.newtech.note.exception;

import com.newtech.note.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestControllerAdvice
public class NoteGlobalExceptionHandler {
    private static final String UNEXPECTED_ERROR_BODY = "An unexpected error occurred";
    private static final Map<String, PublicBusinessError> PUBLIC_BUSINESS_ERRORS = Map.ofEntries(
            publicError("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Unauthorized"),
            publicError("NOTE_FORBIDDEN", HttpStatus.FORBIDDEN, "Note access is forbidden"),
            publicError("NOTE_NOT_FOUND", HttpStatus.NOT_FOUND, "Note was not found"),
            publicError("ANALYSIS_RECORD_NOT_FOUND", HttpStatus.NOT_FOUND,
                    "Analysis record was not found"),
            publicError("ASSIST_RECORD_NOT_FOUND", HttpStatus.NOT_FOUND,
                    "Assist record was not found"),
            publicError("NOTE_ID_REQUIRED", HttpStatus.BAD_REQUEST, "Note id is required"),
            publicError("ANALYSIS_RECORD_ID_REQUIRED", HttpStatus.BAD_REQUEST,
                    "Analysis record id is required"),
            publicError("ASSIST_RECORD_ID_REQUIRED", HttpStatus.BAD_REQUEST,
                    "Assist record id is required"),
            publicError("WEB_SEARCH_QUERY_INVALID", HttpStatus.BAD_REQUEST,
                    "Web search requires a non-empty query"),
            publicError("PUBLIC_SEARCH_QUERY_INVALID", HttpStatus.BAD_REQUEST,
                    "Public web search requires a non-empty query"),
            publicError("RELATED_NOTES_PROVIDER_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                    "相关备忘录分析暂不可用"),
            publicError("WEB_SEARCH_PROVIDERS_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                    "Coze and public web search providers are unavailable"),
            publicError("PUBLIC_SEARCH_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                    "Public web search could not be completed"),
            publicError("PUBLIC_SEARCH_EMPTY", HttpStatus.SERVICE_UNAVAILABLE,
                    "Public web search returned no valid HTTP results"),
            publicError("SEARCH_KEYWORDS_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                    "Search keywords are unavailable"),
            publicError("RELATED_NOTES_TIMEOUT", HttpStatus.GATEWAY_TIMEOUT,
                    "Related-note analysis timed out"),
            publicError("RELATED_NOTES_SNAPSHOT_MISSING", HttpStatus.UNPROCESSABLE_ENTITY,
                    "Related-note analysis snapshot is unavailable"),
            publicError("PUBLIC_SEARCH_ENDPOINT_INVALID", HttpStatus.BAD_GATEWAY,
                    "Public web search endpoint is not allowed"),
            publicError("PUBLIC_SEARCH_REDIRECT_INVALID", HttpStatus.BAD_GATEWAY,
                    "Public web search returned an unsafe redirect"),
            publicError("PUBLIC_SEARCH_REDIRECT_LOOP", HttpStatus.BAD_GATEWAY,
                    "Public web search returned a redirect loop"),
            publicError("PUBLIC_SEARCH_REDIRECT_LIMIT", HttpStatus.BAD_GATEWAY,
                    "Public web search exceeded the redirect limit"),
            publicError("PUBLIC_SEARCH_HTTP_ERROR", HttpStatus.BAD_GATEWAY,
                    "Public web search request failed"));

    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<String>> handleBusinessException(BusinessException exception) {
        return responseFor(exception);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<String>> handleResponseStatusException(
            ResponseStatusException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(UNEXPECTED_ERROR_BODY));
        }
        return Mono.just(ResponseEntity.status(status).body(status.getReasonPhrase()));
    }

    /** Shared fixed-response contract for errors raised before controller advice can intercept. */
    public Mono<ResponseEntity<String>> responseFor(Throwable failure) {
        String code = failure instanceof BusinessException businessException
                ? businessException.getCode()
                : null;
        PublicBusinessError publicError = code == null ? null : PUBLIC_BUSINESS_ERRORS.get(code);
        if (publicError == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(UNEXPECTED_ERROR_BODY));
        }
        return Mono.just(ResponseEntity.status(publicError.status()).body(publicError.body()));
    }

    private static Map.Entry<String, PublicBusinessError> publicError(
            String code, HttpStatus status, String body) {
        return Map.entry(code, new PublicBusinessError(status, body));
    }

    // 处理普通的 HTTP 请求异常
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ResponseEntity<String>> handleException(Exception e) {
        return responseFor(e);
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ResponseEntity<String>> handleThrowable(Throwable throwable) {
        return responseFor(throwable);
    }

    // 处理 SSE 请求的异常
    @ExceptionHandler(ServerWebInputException.class)
    @ResponseBody
    public Flux<String> handleSseException(ServerWebInputException e, ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        return Flux.just("event: error\ndata: Invalid request\n\n");
    }

    private record PublicBusinessError(HttpStatus status, String body) {
    }
}
