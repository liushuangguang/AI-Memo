package com.newtech.note.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Claim;
import com.newtech.note.common.BusinessException;
import com.newtech.note.service.UserService;
import com.newtech.note.util.JWTUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Resolves identity exclusively from the current validated request. */
@Component
public class RequestIdentityService {
    private static final String MOBILE_PREFIX = "Mobile";

    private final JWTUtils jwtUtils;
    private final UserService userService;
    private final boolean guestEnabled;

    public RequestIdentityService(JWTUtils jwtUtils,
                                  UserService userService,
                                  @Value("${app.guest.enabled:false}") boolean guestEnabled) {
        this.jwtUtils = jwtUtils;
        this.userService = userService;
        this.guestEnabled = guestEnabled;
    }

    public Mono<RequestIdentity> resolve(ServerHttpRequest request) {
        String authorization = request == null
                ? null
                : request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (GuestIdentityValidator.isGuestAuthorization(authorization)) {
            return Mono.fromCallable(() -> RequestIdentity.guest(
                    GuestIdentityValidator.requireValidGuest(request, guestEnabled)));
        }
        if (authorization == null || !authorization.startsWith(MOBILE_PREFIX)) {
            return Mono.error(unauthorized());
        }

        String token = authorization.substring(MOBILE_PREFIX.length());
        if (token.isEmpty() || containsUnsafeWhitespace(token)) {
            return Mono.error(unauthorized());
        }

        final DecodedJWT decoded;
        try {
            decoded = jwtUtils.checkJwt(token);
        } catch (Exception failure) {
            return Mono.error(unauthorized());
        }
        Claim uidClaim = decoded.getClaim("uid");
        Long claimedUid = uidClaim == null ? null : uidClaim.asLong();
        if (claimedUid == null || claimedUid <= 0) {
            return Mono.error(unauthorized());
        }

        return userService.getUidByToken(token)
                .filter(authoritativeUid -> authoritativeUid != null && authoritativeUid > 0
                        && Objects.equals(authoritativeUid, claimedUid))
                .map(uid -> RequestIdentity.mobile(uid, token, decoded))
                .switchIfEmpty(Mono.error(unauthorized()));
    }

    private boolean containsUnsafeWhitespace(String value) {
        return value.codePoints().anyMatch(codePoint -> Character.isWhitespace(codePoint)
                || Character.isSpaceChar(codePoint)
                || Character.isISOControl(codePoint));
    }

    private BusinessException unauthorized() {
        return new BusinessException("UNAUTHORIZED", "A valid current authorization is required");
    }

    public record RequestIdentity(String ownerId, Long uid, String token, DecodedJWT jwt, boolean guest) {
        static RequestIdentity guest(String deviceId) {
            return new RequestIdentity(deviceId, null, null, null, true);
        }

        static RequestIdentity mobile(Long uid, String token, DecodedJWT jwt) {
            return new RequestIdentity(String.valueOf(uid), uid, token, jwt, false);
        }
    }
}
