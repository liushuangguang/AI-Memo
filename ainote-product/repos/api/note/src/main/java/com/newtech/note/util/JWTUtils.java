package com.newtech.note.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.newtech.note.common.BusinessException;
import com.newtech.note.entity.dto.UserInfo;
import com.newtech.note.entity.dto.UserInfoDTO;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JWTUtils {

    private static final int MINIMUM_SIGNING_KEY_LENGTH = 32;

    @Value("${jwt.key:}")
    private String jwtKey;

    private static final int TOKEN_TIME_OUT = 3_600;
    private static final int REFRESH_TIME = 300;

    @PostConstruct
    void validateSigningKey() {
        String configuredKey = jwtKey == null ? "" : jwtKey.trim();
        if (configuredKey.length() < MINIMUM_SIGNING_KEY_LENGTH
                || "test".equalsIgnoreCase(configuredKey)) {
            throw new IllegalStateException(
                    "JWT signing key must be explicitly configured with at least 32 characters");
        }
        jwtKey = configuredKey;
    }

    public String createJwt(UserInfo user) {
        return createJwt(user.getUid(), user.getMobile());
    }

    public String createJwt(UserInfoDTO user) {
        return createJwt(user.getUid(), user.getMobile());
    }

    private String createJwt(Long uid, String mobile) {
        Instant issuedAt = Instant.now();
        return JWT.create()
                .withClaim("uid", uid)
                .withClaim("mobile", mobile)
                .withIssuedAt(Date.from(issuedAt))
                .withExpiresAt(Date.from(issuedAt.plusSeconds(TOKEN_TIME_OUT)))
                .sign(Algorithm.HMAC256(jwtKey));
    }

    public DecodedJWT checkJwt(String token) {
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(jwtKey)).build();
            DecodedJWT decodedJWT = verifier.verify(token);
            Date expiresAt = decodedJWT.getExpiresAt();
            if (expiresAt == null || !expiresAt.after(new Date())) {
                throw unauthorized();
            }
            return decodedJWT;
        } catch (BusinessException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw unauthorized();
        }
    }

    public DecodedJWT getDecodedJWT(String token) {
        return checkJwt(token);
    }

    public int verifyToken(Map<String, Claim> claims) {
        try {
            Claim expirationClaim = claims == null ? null : claims.get("exp");
            Date expirationDate = expirationClaim == null ? null : expirationClaim.asDate();
            if (expirationDate == null) {
                throw unauthorized();
            }
            long remainingTime = expirationDate.getTime() - System.currentTimeMillis();
            if (remainingTime <= 0) {
                throw unauthorized();
            }
            return remainingTime <= REFRESH_TIME * 1_000L ? -1 : 0;
        } catch (BusinessException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw unauthorized();
        }
    }

    private BusinessException unauthorized() {
        return new BusinessException("UNAUTHORIZED", "A valid unexpired token is required");
    }
}
