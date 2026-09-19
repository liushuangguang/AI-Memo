package com.newtech.note.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.newtech.note.common.BusinessException;
import com.newtech.note.entity.dto.UserInfo;
import com.newtech.note.entity.dto.UserInfoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JWTUtilsTest {
    private static final String KEY = "jwt-utils-test-signing-key-32-chars";

    private JWTUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JWTUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtKey", KEY);
        jwtUtils.validateSigningKey();
    }

    @Test
    void startupRejectsMissingDefaultTestAndShortKeys() {
        assertRejectedKey(null);
        assertRejectedKey("   ");
        assertRejectedKey("test");
        assertRejectedKey("short-signing-key");
    }

    @Test
    void startupAcceptsExplicitKeyOfAtLeastThirtyTwoCharactersAndCanSign() {
        JWTUtils configured = new JWTUtils();
        ReflectionTestUtils.setField(configured, "jwtKey", "12345678901234567890123456789012");
        configured.validateSigningKey();

        UserInfo user = new UserInfo();
        user.setUid(99L);
        user.setMobile("13800000299");
        DecodedJWT decodedJWT = configured.checkJwt(configured.createJwt(user));

        assertThat(decodedJWT.getClaim("uid").asLong()).isEqualTo(99L);
    }

    @Test
    void bothUserTypesIssueStandardOneHourTokens() {
        UserInfo userInfo = new UserInfo();
        userInfo.setUid(42L);
        userInfo.setMobile("13800000200");
        UserInfoDTO userInfoDTO = new UserInfoDTO(43L, "13800000201", null);

        assertOneHourToken(jwtUtils.createJwt(userInfo), 42L, "13800000200");
        assertOneHourToken(jwtUtils.createJwt(userInfoDTO), 43L, "13800000201");
    }

    @Test
    void validAndRefreshWindowTokensUseExpClaim() {
        DecodedJWT valid = jwtUtils.checkJwt(signedToken(
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(301)));
        DecodedJWT refresh = jwtUtils.checkJwt(signedToken(
                Instant.now().minusSeconds(3_300), Instant.now().plusSeconds(299)));

        assertThat(jwtUtils.verifyToken(valid.getClaims())).isZero();
        assertThat(jwtUtils.verifyToken(refresh.getClaims())).isEqualTo(-1);
    }

    @Test
    void expiredMissingInvalidAndTamperedTokensAreUnauthorized() {
        String expired = signedToken(
                Instant.now().minusSeconds(3_601), Instant.now().minusSeconds(1));
        String missingExp = JWT.create()
                .withClaim("uid", 42L)
                .withClaim("mobile", "13800000200")
                .withIssuedAt(new Date())
                .sign(Algorithm.HMAC256(KEY));
        String invalidExp = JWT.create()
                .withClaim("uid", 42L)
                .withClaim("mobile", "13800000200")
                .withClaim("exp", "not-a-date")
                .sign(Algorithm.HMAC256(KEY));
        String valid = signedToken(Instant.now(), Instant.now().plusSeconds(3_600));

        assertUnauthorized(() -> jwtUtils.checkJwt(expired));
        assertUnauthorized(() -> jwtUtils.checkJwt(missingExp));
        assertUnauthorized(() -> jwtUtils.checkJwt(invalidExp));
        assertUnauthorized(() -> jwtUtils.checkJwt(tamper(valid)));
        assertUnauthorized(() -> jwtUtils.verifyToken(JWT.decode(missingExp).getClaims()));
    }

    private void assertOneHourToken(String token, long uid, String mobile) {
        DecodedJWT decodedJWT = jwtUtils.checkJwt(token);
        assertThat(decodedJWT.getClaim("uid").asLong()).isEqualTo(uid);
        assertThat(decodedJWT.getClaim("mobile").asString()).isEqualTo(mobile);
        assertThat(decodedJWT.getIssuedAt()).isNotNull();
        assertThat(decodedJWT.getExpiresAt()).isNotNull();
        assertThat(decodedJWT.getExpiresAt().getTime() - decodedJWT.getIssuedAt().getTime())
                .isEqualTo(3_600_000L);
    }

    private String signedToken(Instant issuedAt, Instant expiresAt) {
        return JWT.create()
                .withClaim("uid", 42L)
                .withClaim("mobile", "13800000200")
                .withIssuedAt(Date.from(issuedAt))
                .withExpiresAt(Date.from(expiresAt))
                .sign(Algorithm.HMAC256(KEY));
    }

    private String tamper(String token) {
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        return token.substring(0, signatureStart) + replacement
                + token.substring(signatureStart + 1);
    }

    private void assertUnauthorized(Runnable operation) {
        assertThatThrownBy(operation::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo("UNAUTHORIZED"));
    }

    private void assertRejectedKey(String key) {
        JWTUtils unconfigured = new JWTUtils();
        ReflectionTestUtils.setField(unconfigured, "jwtKey", key);
        assertThatThrownBy(unconfigured::validateSigningKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT signing key must be explicitly configured with at least 32 characters");
    }
}
