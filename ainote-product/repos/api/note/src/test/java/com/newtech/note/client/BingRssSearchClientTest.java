package com.newtech.note.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import com.newtech.note.common.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;

class BingRssSearchClientTest {

    @Test
    void parsesLimitedHttpResultsAndRejectsUnsafeLinks() {
        AtomicReference<String> requestedUrl = new AtomicReference<>();
        WebClient webClient = WebClient.builder().exchangeFunction(request -> {
            requestedUrl.set(request.url().toString());
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "application/rss+xml")
                    .body("""
                            <?xml version="1.0" encoding="UTF-8"?>
                            <rss version="2.0"><channel>
                              <item><title>Good</title><link>https://example.com/a</link><description>Summary</description></item>
                              <item><title>Unsafe</title><link>javascript:alert(1)</link><description>No</description></item>
                              <item><title>Second</title><link>http://example.org/b</link><description>Two</description></item>
                            </channel></rss>
                            """)
                    .build());
        }).build();
        BingRssSearchClient client = new BingRssSearchClient(
                webClient, "https://www.bing.com/search", 2, 2);

        StepVerifier.create(client.search("成都 火锅"))
                .assertNext(results -> {
                    assertThat(results).extracting("url").containsExactly(
                            "https://example.com/a", "http://example.org/b");
                })
                .verifyComplete();

        assertThat(requestedUrl.get()).contains("format=rss").contains("q=");
    }

    @Test
    void followsSafeRedirectAndParsesRssResponse() {
        AtomicInteger calls = new AtomicInteger();
        WebClient webClient = WebClient.builder().exchangeFunction(request -> {
            if (calls.getAndIncrement() == 0) {
                assertThat(request.url().getHost()).isEqualTo("www.bing.com");
                return Mono.just(ClientResponse.create(HttpStatus.FOUND)
                        .header("Location", "https://cn.bing.com/search?format=rss&q=test")
                        .build());
            }
            assertThat(request.url().getHost()).isEqualTo("cn.bing.com");
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "text/xml")
                    .body("""
                            <rss version="2.0"><channel><item><title>Result</title>
                            <link>https://example.com/result</link><description>Summary</description>
                            </item></channel></rss>
                            """)
                    .build());
        }).build();

        StepVerifier.create(new BingRssSearchClient(
                        webClient, "https://www.bing.com/search", 2, 2).search("test"))
                .assertNext(results -> assertThat(results).hasSize(1))
                .verifyComplete();
        assertThat(calls).hasValue(2);
    }

    @Test
    void rejectsUnsafeRedirectLocation() {
        WebClient webClient = WebClient.builder().exchangeFunction(request -> Mono.just(
                ClientResponse.create(HttpStatus.FOUND)
                        .header("Location", "file:///etc/passwd")
                        .build())).build();

        StepVerifier.create(new BingRssSearchClient(
                        webClient, "https://www.bing.com/search", 2, 2).search("test"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) error).getCode())
                            .isEqualTo("PUBLIC_SEARCH_REDIRECT_INVALID");
                })
                .verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 429, 500, 502, 599})
    void normalizesEveryUpstreamHttpFailureToOneFixedCode(int status) {
        WebClient webClient = WebClient.builder().exchangeFunction(request -> Mono.just(
                ClientResponse.create(HttpStatusCode.valueOf(status)).build())).build();

        StepVerifier.create(new BingRssSearchClient(
                        webClient, "https://www.bing.com/search", 2, 2).search("test"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    BusinessException businessException = (BusinessException) error;
                    assertThat(businessException.getCode()).isEqualTo("PUBLIC_SEARCH_HTTP_ERROR");
                    assertThat(businessException.getMessage())
                            .isEqualTo("Public web search request failed");
                })
                .verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://127.0.0.1/search",
            "https://[::1]/search",
            "https://10.0.0.1/search",
            "https://172.16.0.1/search",
            "https://192.168.1.5/search",
            "https://169.254.169.254/latest/meta-data",
            "https://8.8.8.8/search",
            "https://service.local/search",
            "https://localhost/search",
            "https://example.org/search"
    })
    void rejectsRedirectsToLocalIpMetadataAndExternalHosts(String location) {
        WebClient webClient = redirectingClient(location);

        StepVerifier.create(new BingRssSearchClient(
                        webClient, "https://www.bing.com/search", 2, 2).search("test"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) error).getCode())
                            .isEqualTo("PUBLIC_SEARCH_REDIRECT_INVALID");
                })
                .verify();
    }

    @Test
    void rejectsHttpsToHttpDowngradeEvenWithinBing() {
        WebClient webClient = redirectingClient("http://cn.bing.com/search?format=rss&q=test");

        StepVerifier.create(new BingRssSearchClient(
                        webClient, "https://www.bing.com/search", 2, 2).search("test"))
                .expectErrorSatisfies(error -> assertThat(((BusinessException) error).getCode())
                        .isEqualTo("PUBLIC_SEARCH_REDIRECT_INVALID"))
                .verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://www.bing.com/search",
            "https://www.bing.com:8443/search",
            "https://example.org/search",
            "https://127.0.0.1/search",
            "https://[::1]/search",
            "https://10.0.0.1/search",
            "https://169.254.169.254/latest/meta-data",
            "https://localhost/search",
            "https://service.local/search",
            "https://metadata.google.internal/search"
    })
    void rejectsUnsafeInitialEndpointBeforeNetworkRequest(String endpoint) {
        AtomicInteger calls = new AtomicInteger();
        WebClient webClient = WebClient.builder().exchangeFunction(request -> {
            calls.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        }).build();

        StepVerifier.create(new BingRssSearchClient(webClient, endpoint, 2, 2).search("test"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) error).getCode())
                            .isEqualTo("PUBLIC_SEARCH_ENDPOINT_INVALID");
                })
                .verify();
        assertThat(calls).hasValue(0);
    }

    @Test
    void rejectsRedirectWithoutLocation() {
        WebClient webClient = WebClient.builder().exchangeFunction(request -> Mono.just(
                ClientResponse.create(HttpStatus.FOUND).build())).build();

        StepVerifier.create(new BingRssSearchClient(
                        webClient, "https://www.bing.com/search", 2, 2).search("test"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) error).getCode())
                            .isEqualTo("PUBLIC_SEARCH_REDIRECT_INVALID");
                })
                .verify();
    }

    @Test
    void rejectsImmediateSelfRedirectAsLoop() {
        AtomicInteger calls = new AtomicInteger();
        WebClient webClient = WebClient.builder().exchangeFunction(request -> {
            calls.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.FOUND)
                    .header("Location", request.url().toString())
                    .build());
        }).build();

        StepVerifier.create(new BingRssSearchClient(
                        webClient, "https://www.bing.com/search", 2, 2).search("test"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) error).getCode())
                            .isEqualTo("PUBLIC_SEARCH_REDIRECT_LOOP");
                })
                .verify();
        assertThat(calls).hasValue(1);
    }

    @Test
    void rejectsTwoNodeRedirectLoop() {
        AtomicInteger calls = new AtomicInteger();
        WebClient webClient = WebClient.builder().exchangeFunction(request -> {
            int call = calls.getAndIncrement();
            String location = call == 0
                    ? "https://www.bing.com/second"
                    : "https://www.bing.com/search?format=rss&q=test";
            return Mono.just(ClientResponse.create(HttpStatus.FOUND)
                    .header("Location", location)
                    .build());
        }).build();

        assertRedirectFailure(webClient, "https://www.bing.com/search",
                "PUBLIC_SEARCH_REDIRECT_LOOP");
        assertThat(calls).hasValue(2);
    }

    @Test
    void rejectsRelativeRedirectLoop() {
        AtomicInteger calls = new AtomicInteger();
        WebClient webClient = WebClient.builder().exchangeFunction(request -> {
            calls.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.FOUND)
                    .header("Location", "/search?format=rss&q=test")
                    .build());
        }).build();

        assertRedirectFailure(webClient, "https://www.bing.com/search",
                "PUBLIC_SEARCH_REDIRECT_LOOP");
        assertThat(calls).hasValue(1);
    }

    @Test
    void canonicalizesHostCaseTrailingDotAndDefaultPortForLoopDetection() {
        AtomicInteger calls = new AtomicInteger();
        WebClient webClient = WebClient.builder().exchangeFunction(request -> {
            calls.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.FOUND)
                    .header("Location", "https://WWW.BING.COM.:443/search?format=rss&q=test")
                    .build());
        }).build();

        assertRedirectFailure(webClient, "https://www.bing.com/search",
                "PUBLIC_SEARCH_REDIRECT_LOOP");
        assertThat(calls).hasValue(1);
    }

    @Test
    void canonicalizesUnreservedPercentEncodingAndEncodedDotSegments() {
        AtomicInteger unreservedCalls = new AtomicInteger();
        WebClient unreservedClient = WebClient.builder().exchangeFunction(request -> {
            unreservedCalls.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.FOUND)
                    .header("Location", "/%7euser?format=rss&q=test")
                    .build());
        }).build();
        assertRedirectFailure(unreservedClient, "https://www.bing.com/~user",
                "PUBLIC_SEARCH_REDIRECT_LOOP");
        assertThat(unreservedCalls).hasValue(1);

        AtomicInteger dotCalls = new AtomicInteger();
        WebClient dotClient = WebClient.builder().exchangeFunction(request -> {
            dotCalls.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.FOUND)
                    .header("Location", "/a/%2e%2e/b?format=rss&q=test")
                    .build());
        }).build();
        assertRedirectFailure(dotClient, "https://www.bing.com/b",
                "PUBLIC_SEARCH_REDIRECT_LOOP");
        assertThat(dotCalls).hasValue(1);
    }

    @Test
    void canonicalizesPercentEscapeHexCase() {
        AtomicInteger calls = new AtomicInteger();
        WebClient webClient = WebClient.builder().exchangeFunction(request -> {
            int call = calls.getAndIncrement();
            return Mono.just(ClientResponse.create(HttpStatus.FOUND)
                    .header("Location", call == 0
                            ? "/path%2Fpart?format=rss&q=test"
                            : "/path%2fpart?format=rss&q=test")
                    .build());
        }).build();

        assertRedirectFailure(webClient, "https://www.bing.com/search",
                "PUBLIC_SEARCH_REDIRECT_LOOP");
        assertThat(calls).hasValue(2);
    }

    @Test
    void independentlyRejectsFourthUniqueRedirect() {
        AtomicInteger calls = new AtomicInteger();
        WebClient webClient = WebClient.builder().exchangeFunction(request -> {
            int call = calls.getAndIncrement();
            return Mono.just(ClientResponse.create(HttpStatus.FOUND)
                    .header("Location", "https://www.bing.com/hop-" + call)
                    .build());
        }).build();

        assertRedirectFailure(webClient, "https://www.bing.com/search",
                "PUBLIC_SEARCH_REDIRECT_LIMIT");
        assertThat(calls).hasValue(4);
    }

    private WebClient redirectingClient(String location) {
        return WebClient.builder().exchangeFunction(request -> Mono.just(
                ClientResponse.create(HttpStatus.FOUND)
                        .header("Location", location)
                        .build())).build();
    }

    private void assertRedirectFailure(WebClient webClient, String endpoint, String code) {
        StepVerifier.create(new BingRssSearchClient(webClient, endpoint, 2, 2).search("test"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) error).getCode()).isEqualTo(code);
                })
                .verify();
    }
}
