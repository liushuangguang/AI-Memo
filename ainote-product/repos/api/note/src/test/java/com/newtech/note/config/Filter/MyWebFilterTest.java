package com.newtech.note.config.Filter;

import com.newtech.note.exception.NoteGlobalExceptionHandler;
import com.newtech.note.service.UserService;
import com.newtech.note.security.RequestIdentityService;
import com.newtech.note.util.JWTUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class MyWebFilterTest {

    private static final String SPOOFED_SWAGGER_REFERER =
            "https://attacker.example/note/webjars/swagger-ui/index.html";

    @Test
    void rejectsMissingAuthorizationEvenWithSpoofedSwaggerReferer() {
        for (boolean guestEnabled : new boolean[]{false, true}) {
            MyWebFilter filter = filter(guestEnabled);
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/note/analysis/record/latest")
                            .header("Referer", SPOOFED_SWAGGER_REFERER));

            assertResponseStatus(filter.filter(exchange, ignored -> Mono.empty()),
                    exchange, HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    void improveRoutesRejectUnauthenticatedRequestsEvenWithSpoofedReferer() {
        MyWebFilter filter = filter(true);
        for (String path : new String[]{"/note/improve", "/note/improve/completeInfo"}) {
            MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post(path)
                    .header("Referer", SPOOFED_SWAGGER_REFERER));
            assertResponseStatus(filter.filter(exchange, ignored -> Mono.empty()),
                    exchange, HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    void leavesSwaggerUiResourcesAndApiDocsPublic() {
        MyWebFilter filter = filter(false);

        for (String path : new String[]{
                "/v1/note/swagger-ui.html",
                "/note/webjars/swagger-ui/index.html",
                "/v1/note-docs"}) {
            AtomicBoolean chainInvoked = new AtomicBoolean();
            MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path));

            filter.filter(exchange, ignored -> {
                chainInvoked.set(true);
                return Mono.empty();
            }).block();

            assertTrue(chainInvoked.get(), () -> "Expected public Swagger path: " + path);
        }
    }

    @Test
    void acceptsGuestAuthorizationWhenGuestModeIsEnabled() {
        JWTUtils jwtUtils = mock(JWTUtils.class);
        UserService userService = mock(UserService.class);
        MyWebFilter filter = new MyWebFilter(
                new RequestIdentityService(jwtUtils, userService, true),
                new NoteGlobalExceptionHandler());
        MockServerWebExchange exchange = exchange("/points/get", "Guest test-device");
        AtomicBoolean chainInvoked = new AtomicBoolean();
        WebFilterChain chain = ignored -> {
            chainInvoked.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertTrue(chainInvoked.get());
        verifyNoInteractions(jwtUtils, userService);
    }

    @Test
    void replacesClientSuppliedOwnerHeaderWithTheValidatedGuestIdentity() {
        MyWebFilter filter = filter(true);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/v2/note/list")
                .header("Authorization", "Guest guest-a")
                .header("Device-Id", "guest-a")
                .header(MyWebFilter.AUTHENTICATED_OWNER_HEADER, "attacker"));

        filter.filter(exchange, authenticatedExchange -> {
            assertEquals("guest-a", authenticatedExchange.getRequest().getHeaders()
                    .getFirst(MyWebFilter.AUTHENTICATED_OWNER_HEADER));
            return Mono.empty();
        }).block();
    }

    @Test
    void authenticatedLegacyAiAndAudioRoutesFailClosedWithGone() {
        MyWebFilter filter = filter(true);
        for (String path : new String[]{
                "/note/analysis/organizedNoteText",
                "/audio/getNoteDiscussAudio"}) {
            AtomicBoolean chainInvoked = new AtomicBoolean();
            Mono<Void> result = filter.filter(exchange(path, "Guest test-device"), ignored -> {
                chainInvoked.set(true);
                return Mono.empty();
            });

            assertStatus(result, HttpStatus.GONE);
            assertTrue(!chainInvoked.get());
        }
    }

    @Test
    void authenticatedLegacyNoteCrudRoutesFailClosedWithGone() {
        MyWebFilter filter = filter(true);

        for (String path : new String[]{
                "/note/list",
                "/note/note-id",
                "/note/create",
                "/note/createImageNote",
                "/note/update/note-id",
                "/note/delete/note-id",
                "/note/appendToOriginalText"}) {
            AtomicBoolean chainInvoked = new AtomicBoolean();
            Mono<Void> result = filter.filter(exchange(path, "Guest test-device"), ignored -> {
                chainInvoked.set(true);
                return Mono.empty();
            });

            assertStatus(result, HttpStatus.GONE);
            assertTrue(!chainInvoked.get(), () -> "Expected disabled legacy route: " + path);
        }
    }

    @Test
    void unauthenticatedLegacyNoteCrudRoutesReturnUnauthorizedBeforeGone() {
        MyWebFilter filter = filter(true);

        for (String path : new String[]{
                "/note/list",
                "/note/note-id",
                "/note/create",
                "/note/createImageNote",
                "/note/update/note-id",
                "/note/delete/note-id",
                "/note/appendToOriginalText"}) {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get(path)
                            .header(MyWebFilter.AUTHENTICATED_OWNER_HEADER, "spoofed-owner"));

            assertResponseStatus(filter.filter(exchange, ignored -> Mono.empty()),
                    exchange, HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    void encodedAndMatrixLegacyCrudVariantsFailClosed() {
        MyWebFilter filter = filter(true);

        for (String path : new String[]{
                "/note;x=1/list;y=2",
                "/note%3Bx=1/list%3By=2",
                "/note%253Bx=1%252Flist%253By=2",
                "/note%2Fupdate%2Fnote-id",
                "/note%252Fdelete%252Fnote-id"}) {
            Mono<Void> result = filter.filter(exchange(path, "Guest test-device"), ignored -> Mono.empty());
            assertStatus(result, HttpStatus.GONE);
        }
    }

    @Test
    void authenticatedGuestsCanReachImproveRoutes() {
        MyWebFilter filter = filter(true);
        for (String path : new String[]{"/note/improve", "/note/improve/completeInfo"}) {
            AtomicBoolean chainInvoked = new AtomicBoolean();
            filter.filter(exchange(path, "Guest test-device"), ignored -> {
                chainInvoked.set(true);
                return Mono.empty();
            }).block();
            assertTrue(chainInvoked.get(), () -> "Expected authenticated improve route: " + path);
        }
    }

    @Test
    void authenticatedGuestsCanReachEveryModernNoteNamespace() {
        MyWebFilter filter = filter(true);

        for (String path : new String[]{
                "/note/analysis/record/latest",
                "/note/assist/rewriteContent",
                "/note/improve/completeInfo",
                "/note/theme/pagination"}) {
            AtomicBoolean chainInvoked = new AtomicBoolean();
            filter.filter(exchange(path, "Guest test-device"), ignored -> {
                chainInvoked.set(true);
                return Mono.empty();
            }).block();
            assertTrue(chainInvoked.get(), () -> "Expected modern route to remain reachable: " + path);
        }
    }

    @Test
    void rejectsGuestAuthorizationWhenGuestModeIsDisabled() {
        MyWebFilter filter = filter(false);
        MockServerWebExchange exchange = exchange("/points/get", "Guest test-device");

        assertResponseStatus(filter.filter(exchange, ignored -> Mono.empty()),
                exchange, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsGuestAuthorizationWithoutDeviceIdentifier() {
        MyWebFilter filter = filter(true);
        MockServerWebExchange exchange = exchange("/points/get", "Guest ");

        assertResponseStatus(filter.filter(exchange, ignored -> Mono.empty()),
                exchange, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsGuestAuthorizationContainingWhitespace() {
        for (String authorization : new String[]{
                "Guest test device",
                "Guest test\tdevice",
                "Guest test-device ",
                "Guest test\u00a0device",
                "Guest test\u3000device"}) {
            MyWebFilter filter = filter(true);
            MockServerWebExchange exchange = exchange("/points/get", authorization);

            assertResponseStatus(filter.filter(exchange, ignored -> Mono.empty()),
                    exchange, HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    void protectsEachConfiguredRootAndChildRoute() {
        MyWebFilter filter = filter(true);

        for (String root : new String[]{
                "/audio",
                "/note/analysis",
                "/v2/note",
                "/v2/capture",
                "/v2/note/analysis",
                "/note/analysis/record",
                "/note/assist",
                "/note/improve",
                "/note/theme",
                "/backlog",
                "/test/note/analysis/todo",
                "/points"}) {
            for (String path : new String[]{root, root + "/completeInfo"}) {
                MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post(path));
                assertResponseStatus(filter.filter(exchange, ignored -> Mono.empty()),
                        exchange, HttpStatus.UNAUTHORIZED);
            }
        }
    }

    @Test
    void leavesAdjacentPathPrefixesUnprotected() {
        MyWebFilter filter = filter(false);

        for (String path : new String[]{
                "/audioBook",
                "/note/analysisExtra/child",
                "/v2/noteExtra",
                "/v2/captureExtra",
                "/notebook/list",
                "/note-list",
                "/note/list/extra",
                "/note/update/id/extra",
                "/note/delete/id/extra",
                "/backlogger",
                "/test/note/analysis/todoExtra",
                "/pointsBalance"}) {
            AtomicBoolean chainInvoked = new AtomicBoolean();
            MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path));

            filter.filter(exchange, ignored -> {
                chainInvoked.set(true);
                return Mono.empty();
            }).block();

            assertTrue(chainInvoked.get(), () -> "Expected adjacent path to remain unprotected: " + path);
        }
    }

    @Test
    void protectsMatrixParameterVariantsOfProtectedPaths() {
        MyWebFilter filter = filter(false);

        for (String path : new String[]{
                "/points;x=1/get",
                "/points%3Bx=1/get",
                "/points%253Bx=1/get",
                "/note;x=1/improve/completeInfo",
                "/note%3Bx=1/improve/completeInfo",
                "/note%253Bx=1/improve/completeInfo"}) {
            MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path));
            assertResponseStatus(filter.filter(exchange, ignored -> Mono.empty()),
                    exchange, HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    void leavesUnprotectedRoutesUnchanged() {
        MyWebFilter filter = filter(false);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/auth/login"));
        AtomicBoolean chainInvoked = new AtomicBoolean();

        filter.filter(exchange, ignored -> {
            chainInvoked.set(true);
            return Mono.empty();
        }).block();

        assertTrue(chainInvoked.get());
    }

    private MockServerWebExchange exchange(String path, String authorization) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get(path)
                .header("Authorization", authorization);
        if (authorization.startsWith("Guest ")) {
            builder.header("Device-Id", authorization.substring("Guest ".length()));
        }
        return MockServerWebExchange.from(builder);
    }

    private MyWebFilter filter(boolean guestEnabled) {
        return new MyWebFilter(new RequestIdentityService(
                mock(JWTUtils.class), mock(UserService.class), guestEnabled),
                new NoteGlobalExceptionHandler());
    }

    private void assertStatus(Mono<Void> result, HttpStatus expected) {
        StepVerifier.create(result)
                .expectErrorSatisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertEquals(expected, exception.getStatusCode());
                })
                .verify();
    }

    private void assertResponseStatus(Mono<Void> result,
                                      MockServerWebExchange exchange,
                                      HttpStatus expected) {
        StepVerifier.create(result).verifyComplete();
        assertEquals(expected, exchange.getResponse().getStatusCode());
    }
}
