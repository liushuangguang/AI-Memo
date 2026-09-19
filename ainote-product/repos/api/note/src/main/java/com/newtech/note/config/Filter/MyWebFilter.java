package com.newtech.note.config.Filter;

import com.newtech.note.exception.NoteGlobalExceptionHandler;
import com.newtech.note.security.RequestIdentityService;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class MyWebFilter implements WebFilter {

    /** Added only after current-request authentication; never trust this from a client request. */
    public static final String AUTHENTICATED_OWNER_HEADER = "X-Authenticated-Owner";
    private final RequestIdentityService identityService;
    private final NoteGlobalExceptionHandler exceptionHandler;
    private static final int MAX_PATH_DECODING_PASSES = 3;
    private static final String[] PROTECTED_PATH_ROOTS = {"/audio", "/note/analysis", "/v2/note", "/v2/capture",
            "/note/analysis/record", "/note/assist", "/note/improve", "/note/theme", "/backlog",
            "/test/note/analysis/todo", "/points"};
    private static final String[] MODERN_NOTE_ROOTS = {"/note/analysis/record", "/note/assist",
            "/note/improve", "/note/theme"};

    public MyWebFilter(RequestIdentityService identityService,
                       NoteGlobalExceptionHandler exceptionHandler) {
        this.identityService = identityService;
        this.exceptionHandler = exceptionHandler;
    }

    @NotNull
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, @NotNull WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (!requiresAuthentication(request)) {
            return chain.filter(exchange);
        }
        Mono<RequestIdentityService.RequestIdentity> resolvedIdentity = Mono
                .defer(() -> identityService.resolve(request))
                .onErrorResume(failure -> writeIdentityFailure(exchange, failure)
                        .then(Mono.empty()));
        return resolvedIdentity
                .flatMap(identity -> isDisabledLegacyRoute(request)
                        ? Mono.error(new ResponseStatusException(HttpStatus.GONE,
                        "This legacy AI/audio route is disabled; use the v2 analysis or assist API"))
                        : chain.filter(withAuthenticatedOwner(exchange, identity.ownerId())));
    }

    private Mono<Void> writeIdentityFailure(ServerWebExchange exchange, Throwable failure) {
        return exceptionHandler.responseFor(failure)
                .flatMap(response -> writeResponse(exchange, response));
    }

    private Mono<Void> writeResponse(ServerWebExchange exchange, ResponseEntity<String> response) {
        byte[] body = response.getBody() == null
                ? new byte[0]
                : response.getBody().getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(response.getStatusCode());
        exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
        exchange.getResponse().getHeaders().setContentLength(body.length);
        return exchange.getResponse().writeWith(Mono.just(
                exchange.getResponse().bufferFactory().wrap(body)));
    }

    private ServerWebExchange withAuthenticatedOwner(ServerWebExchange exchange, String ownerId) {
        ServerHttpRequest authenticatedRequest = exchange.getRequest().mutate()
                .headers(headers -> headers.set(AUTHENTICATED_OWNER_HEADER, ownerId))
                .build();
        return exchange.mutate().request(authenticatedRequest).build();
    }

    private boolean isDisabledLegacyRoute(ServerHttpRequest request) {
        String path = canonicalPath(request);
        return isLegacyNoteCrudRoute(path)
                || (isPathAtOrBelowRoot(path, "/note/analysis")
                && !isModernNoteRoute(path))
                || isPathAtOrBelowRoot(path, "/audio");
    }

    private boolean isLegacyNoteCrudRoute(String path) {
        if (isModernNoteRoute(path)) {
            return false;
        }
        // The legacy GET /note/{id} mapping also covers every one-segment CRUD operation name.
        return isSingleSegmentBelow(path, "/note")
                || isSingleSegmentBelow(path, "/note/update")
                || isSingleSegmentBelow(path, "/note/delete");
    }

    private boolean isModernNoteRoute(String path) {
        return Arrays.stream(MODERN_NOTE_ROOTS)
                .anyMatch(root -> isPathAtOrBelowRoot(path, root));
    }

    private boolean isSingleSegmentBelow(String path, String root) {
        if (!path.startsWith(root + "/")) {
            return false;
        }
        String remainder = path.substring(root.length() + 1);
        return !remainder.isEmpty() && remainder.indexOf('/') < 0;
    }

    private boolean isPathAtOrBelowRoot(String path, String root) {
        return path.equals(root) || path.startsWith(root + "/");
    }

    private boolean requiresAuthentication(ServerHttpRequest request) {
        String normalizedPath = canonicalPath(request);
        return isLegacyNoteCrudRoute(normalizedPath)
                || Arrays.stream(PROTECTED_PATH_ROOTS)
                .anyMatch(root -> isPathAtOrBelowRoot(normalizedPath, root));
    }

    private String canonicalPath(ServerHttpRequest request) {
        String candidate = request.getURI().getRawPath();
        for (int decodingLevel = 0; decodingLevel <= MAX_PATH_DECODING_PASSES; decodingLevel++) {
            String normalizedPath = stripMatrixParameters(candidate);
            if (!containsEncodedOctet(candidate)) {
                return normalizedPath;
            }
            if (decodingLevel == MAX_PATH_DECODING_PASSES) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "路径编码层级过多");
            }

            try {
                candidate = UriUtils.decode(candidate, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "路径编码无效", exception);
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "路径编码无效");
    }

    private String stripMatrixParameters(String path) {
        return Arrays.stream(path.split("/", -1))
                .map(segment -> {
                    int parameterStart = segment.indexOf(';');
                    return parameterStart >= 0 ? segment.substring(0, parameterStart) : segment;
                })
                .reduce((left, right) -> left + "/" + right)
                .orElse("");
    }

    private boolean containsEncodedOctet(String path) {
        for (int index = 0; index + 2 < path.length(); index++) {
            if (path.charAt(index) == '%'
                    && Character.digit(path.charAt(index + 1), 16) >= 0
                    && Character.digit(path.charAt(index + 2), 16) >= 0) {
                return true;
            }
        }
        return false;
    }

}
