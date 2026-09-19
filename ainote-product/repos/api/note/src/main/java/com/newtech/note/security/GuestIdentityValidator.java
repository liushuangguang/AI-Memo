package com.newtech.note.security;

import com.newtech.note.common.BusinessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

/** Strict, shared validation for the debug-only Guest authentication scheme. */
public final class GuestIdentityValidator {
    private static final String PREFIX = "Guest ";

    private GuestIdentityValidator() {
    }

    public static boolean isGuestAuthorization(String authorization) {
        return authorization != null && authorization.startsWith("Guest");
    }

    public static String requireValidGuest(ServerHttpRequest request, boolean guestEnabled) {
        if (!guestEnabled || request == null) {
            throw unauthorized();
        }
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String deviceId = request.getHeaders().getFirst("Device-Id");
        if (authorization == null || !authorization.startsWith(PREFIX)
                || authorization.length() == PREFIX.length()) {
            throw unauthorized();
        }

        String guestId = authorization.substring(PREFIX.length());
        if (containsUnsafeWhitespace(guestId) || isPositiveDecimalLong(guestId)
                || !guestId.equals(deviceId)) {
            throw unauthorized();
        }
        return guestId;
    }

    private static boolean isPositiveDecimalLong(String value) {
        if (!value.codePoints().allMatch(codePoint -> codePoint >= '0' && codePoint <= '9')) {
            return false;
        }
        try {
            return Long.parseLong(value) > 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean containsUnsafeWhitespace(String value) {
        return value.codePoints().anyMatch(codePoint -> Character.isWhitespace(codePoint)
                || Character.isSpaceChar(codePoint)
                || Character.isISOControl(codePoint));
    }

    private static BusinessException unauthorized() {
        return new BusinessException("UNAUTHORIZED",
                "A valid enabled Guest authorization matching Device-Id is required");
    }
}
