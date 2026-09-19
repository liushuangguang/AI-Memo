package com.newtech.note.config.Filter;

import com.newtech.note.common.BusinessException;
import com.newtech.note.exception.NoteGlobalExceptionHandler;
import com.newtech.note.security.RequestIdentityService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MyWebFilterExceptionIntegrationTest {
    private static final String UNEXPECTED_BODY = "An unexpected error occurred";

    @Test
    void asynchronousIdentityUnauthorizedUsesFixed401BodyAndDoesNotInvokeController() {
        RequestIdentityService identityService = mock(RequestIdentityService.class);
        when(identityService.resolve(any())).thenReturn(Mono.error(new BusinessException(
                "UNAUTHORIZED", "sensitive asynchronous identity marker")));

        assertIdentityFailure(identityService, HttpStatus.UNAUTHORIZED, "Unauthorized");
    }

    @Test
    void synchronousIdentityUnauthorizedUsesFixed401BodyAndDoesNotInvokeController() {
        RequestIdentityService identityService = mock(RequestIdentityService.class);
        when(identityService.resolve(any())).thenThrow(new BusinessException(
                "UNAUTHORIZED", "sensitive synchronous identity marker"));

        assertIdentityFailure(identityService, HttpStatus.UNAUTHORIZED, "Unauthorized");
    }

    @Test
    void asynchronousIdentityThrowableUsesFixed500BodyAndDoesNotInvokeController() {
        RequestIdentityService identityService = mock(RequestIdentityService.class);
        when(identityService.resolve(any())).thenReturn(Mono.error(
                new SensitiveThrowable("sensitive asynchronous throwable marker")));

        assertIdentityFailure(identityService, HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_BODY);
    }

    @Test
    void synchronousIdentityThrowableUsesFixed500BodyAndDoesNotInvokeController() {
        RequestIdentityService identityService = mock(RequestIdentityService.class);
        when(identityService.resolve(any())).thenThrow(
                new SensitiveError("sensitive synchronous throwable marker"));

        assertIdentityFailure(identityService, HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_BODY);
    }

    @Test
    void synchronousIdentityNullCodeUsesFixed500BodyAndDoesNotInvokeController() {
        RequestIdentityService identityService = mock(RequestIdentityService.class);
        when(identityService.resolve(any())).thenThrow(new BusinessException(
                null, "sensitive synchronous null-code marker"));

        assertIdentityFailure(identityService, HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_BODY);
    }

    @Test
    void downstreamNullCodeUsesFixed500Contract() {
        RequestIdentityService identityService = authenticatedIdentityService();

        client(identityService, new FailureProbeController()).get().uri("/points/null-code")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(String.class).isEqualTo(UNEXPECTED_BODY);
    }

    @Test
    void downstreamKnownCodeRetainsItsFixedPublicContract() {
        RequestIdentityService identityService = authenticatedIdentityService();

        client(identityService, new FailureProbeController()).get().uri("/points/known")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
                .expectBody(String.class)
                .isEqualTo("Public web search returned an unsafe redirect");
    }

    private void assertIdentityFailure(RequestIdentityService identityService,
                                       HttpStatus status,
                                       String body) {
        FailureProbeController controller = new FailureProbeController();

        client(identityService, controller).get().uri("/points/success")
                .exchange()
                .expectStatus().isEqualTo(status)
                .expectBody(String.class).isEqualTo(body);
        assertThat(controller.calls).hasValue(0);
    }

    @Test
    void downstreamUnknownBusinessExceptionIsNotRewrittenAsAuthenticationFailure() {
        RequestIdentityService identityService = authenticatedIdentityService();

        client(identityService, new FailureProbeController()).get().uri("/points/business")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(String.class).isEqualTo(UNEXPECTED_BODY);
    }

    @Test
    void downstreamNonExceptionThrowableUsesGlobalFixed500Contract() {
        RequestIdentityService identityService = authenticatedIdentityService();

        client(identityService, new FailureProbeController()).get().uri("/points/throwable")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(String.class).isEqualTo(UNEXPECTED_BODY);
    }

    private RequestIdentityService authenticatedIdentityService() {
        RequestIdentityService identityService = mock(RequestIdentityService.class);
        when(identityService.resolve(any())).thenReturn(Mono.just(
                new RequestIdentityService.RequestIdentity(
                        "owner", 1L, null, null, false)));
        return identityService;
    }

    private WebTestClient client(RequestIdentityService identityService, Object controller) {
        NoteGlobalExceptionHandler exceptionHandler = new NoteGlobalExceptionHandler();
        return WebTestClient.bindToController(controller)
                .controllerAdvice(exceptionHandler)
                .webFilter(new MyWebFilter(identityService, exceptionHandler))
                .build();
    }

    @RestController
    private static class FailureProbeController {
        private final AtomicInteger calls = new AtomicInteger();

        @GetMapping("/points/success")
        Mono<String> success() {
            calls.incrementAndGet();
            return Mono.just("ok");
        }

        @GetMapping("/points/business")
        Mono<String> businessFailure() {
            calls.incrementAndGet();
            return Mono.error(new BusinessException(
                    "UNKNOWN_DOWNSTREAM", "sensitive downstream business marker"));
        }

        @GetMapping("/points/null-code")
        Mono<String> nullCodeFailure() {
            calls.incrementAndGet();
            return Mono.error(new BusinessException(
                    null, "sensitive downstream null-code marker"));
        }

        @GetMapping("/points/known")
        Mono<String> knownBusinessFailure() {
            calls.incrementAndGet();
            return Mono.error(new BusinessException(
                    "PUBLIC_SEARCH_REDIRECT_INVALID", "sensitive downstream known marker"));
        }

        @GetMapping("/points/throwable")
        Mono<String> throwableFailure() {
            calls.incrementAndGet();
            return Mono.error(new SensitiveThrowable("sensitive downstream throwable marker"));
        }
    }

    private static final class SensitiveThrowable extends Throwable {
        private SensitiveThrowable(String message) {
            super(message);
        }
    }

    private static final class SensitiveError extends Error {
        private SensitiveError(String message) {
            super(message);
        }
    }
}
