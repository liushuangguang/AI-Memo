package com.newtech.note.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtech.note.client.CozeClient;
import com.newtech.note.client.BingRssSearchClient;
import com.newtech.note.client.DeepSeekClient;
import com.newtech.note.client.DifyClient;
import com.newtech.note.client.DeepSeekProviderException;
import com.newtech.note.service.PointsService;
import com.newtech.note.service.UserService;
import com.newtech.note.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class SearchServiceImplTest {

    @Test
    void extractsKeywordArrayFromDeepSeekFallback() {
        DifyClient difyClient = mock(DifyClient.class);
        DeepSeekClient deepSeekClient = mock(DeepSeekClient.class);
        SearchServiceImpl service = new SearchServiceImpl(
                new ObjectMapper(), difyClient, deepSeekClient, mock(CozeClient.class),
                mock(BingRssSearchClient.class));
        ReflectionTestUtils.setField(service, "apiKeyForSearchKeywords", "");
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), anyString(), any(), any()))
                .thenReturn(Mono.just("{\"keywords\":[\"川菜\",\"成都火锅\",\"川菜\"]}"));

        StepVerifier.create(service.searchKeywords("成都吃什么", null))
                .assertNext(keywords -> assertThat(keywords)
                        .containsExactly("川菜", "成都火锅"))
                .verifyComplete();
    }

    @Test
    void returnsNormalizedCozeMainPathResults() {
        CozeClient cozeClient = mock(CozeClient.class);
        BingRssSearchClient fallback = mock(BingRssSearchClient.class);
        SearchServiceImpl service = service(cozeClient, fallback);
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.just("""
                        {"output":{"webPages":{"value":[
                          {"name":" One ","snippet":" Result ","url":" https://example.com/a ",
                           "ignoredProviderField":{"value":"allowed"}},
                          {"name":"Bad","url":"javascript:alert(1)"},
                          {"name":"Duplicate","url":"https://example.com/a"}
                        ]}}}
                        """));

        StepVerifier.create(service.search("query", null))
                .assertNext(results -> {
                    assertThat(results).hasSize(1);
                    assertThat(results.getFirst().getUrl()).isEqualTo("https://example.com/a");
                    assertThat(results.getFirst().getName()).isEqualTo("One");
                    assertThat(results.getFirst().getSnippet()).isEqualTo("Result");
                })
                .verifyComplete();
        verifyNoInteractions(fallback);
    }

    @Test
    void returnsNormalizedCozeStructuredArrayResults() {
        CozeClient cozeClient = mock(CozeClient.class);
        BingRssSearchClient fallback = mock(BingRssSearchClient.class);
        SearchServiceImpl service = service(cozeClient, fallback);
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.just("""
                        {"output":[
                          {"title":" One ","summary":" Result ","url":" https://example.com/a ",
                           "source":"provider-specific"},
                          {"title":"Bad","summary":"Unsafe","url":"javascript:alert(1)"},
                          {"title":"Duplicate","summary":"Ignored","url":"https://example.com/a"},
                          {"title":"Two","summary":" Second result ","url":"http://example.org/b"}
                        ]}
                        """));

        StepVerifier.create(service.search("query", null))
                .assertNext(results -> {
                    assertThat(results).hasSize(2);
                    assertThat(results).extracting("url")
                            .containsExactly("https://example.com/a", "http://example.org/b");
                    assertThat(results.getFirst().getName()).isEqualTo("One");
                    assertThat(results.getFirst().getSnippet()).isEqualTo("Result");
                    assertThat(results.get(1).getName()).isEqualTo("Two");
                    assertThat(results.get(1).getSnippet()).isEqualTo("Second result");
                })
                .verifyComplete();
        verifyNoInteractions(fallback);
    }

    @Test
    void acceptsJsonWrappedRssAndFiltersUnsafeOrDuplicateLinks() throws Exception {
        CozeClient cozeClient = mock(CozeClient.class);
        BingRssSearchClient fallback = mock(BingRssSearchClient.class);
        SearchServiceImpl service = service(cozeClient, fallback);
        String rss = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0"><channel>
                  <item><title> One </title><description> Result </description>
                    <link> https://example.com/a </link></item>
                  <item><title>Unsafe</title><link>javascript:alert(1)</link></item>
                  <item><title>Duplicate</title><link>https://example.com/a</link></item>
                  <item><title>Two</title><description><![CDATA[Second result]]></description>
                    <link>http://example.org/b</link></item>
                </channel></rss>
                """;
        String response = new ObjectMapper().writeValueAsString(Map.of("output", rss));
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.just(response));

        StepVerifier.create(service.search("query", null))
                .assertNext(results -> {
                    assertThat(results).hasSize(2);
                    assertThat(results).extracting("url")
                            .containsExactly("https://example.com/a", "http://example.org/b");
                    assertThat(results.getFirst().getName()).isEqualTo("One");
                    assertThat(results.getFirst().getSnippet()).isEqualTo("Result");
                    assertThat(results.get(1).getSnippet()).isEqualTo("Second result");
                })
                .verifyComplete();
        verifyNoInteractions(fallback);
    }

    @Test
    void acceptsDuckDuckGoHtmlAndUnwrapsUddgLinks() throws Exception {
        CozeClient cozeClient = mock(CozeClient.class);
        BingRssSearchClient fallback = mock(BingRssSearchClient.class);
        SearchServiceImpl service = service(cozeClient, fallback);
        String html = """
                <!DOCTYPE html>
                <html><body>
                  <div class="result results_links results_links_deep web-result">
                    <h2 class="result__title">
                      <a rel="nofollow" class="result__a"
                         href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com%2Fguide%3Fa%3D1%26b%3D2&amp;rut=abc">
                        <b>AI</b> 备忘录 &amp; 指南
                      </a>
                    </h2>
                    <a class="result__snippet" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com%2Fguide">
                      理解 <b>语义搜索</b> 的说明。
                    </a>
                  </div>
                  <div class="result results_links">
                    <a class="extra result__a" href="https://example.org/second">第二条结果</a>
                    <div class="result__snippet">第二条摘要</div>
                  </div>
                  <a class="result__a"
                     href="/l/?uddg=https%3A%2F%2Fexample.com%2Fguide%3Fa%3D1%26b%3D2">重复项</a>
                  <a class="result__a"
                     href="/l/?uddg=javascript%3Aalert%281%29">不安全项</a>
                </body></html>
                """;
        String response = new ObjectMapper().writeValueAsString(Map.of("output", html));
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.just(response));

        StepVerifier.create(service.search("query", null))
                .assertNext(results -> {
                    assertThat(results).hasSize(2);
                    assertThat(results).extracting("url")
                            .containsExactly("https://example.com/guide?a=1&b=2", "https://example.org/second");
                    assertThat(results.getFirst().getName()).isEqualTo("AI 备忘录 & 指南");
                    assertThat(results.getFirst().getSnippet()).isEqualTo("理解 语义搜索 的说明。");
                    assertThat(results.get(1).getName()).isEqualTo("第二条结果");
                    assertThat(results.get(1).getSnippet()).isEqualTo("第二条摘要");
                })
                .verifyComplete();
        verifyNoInteractions(fallback);
    }

    @Test
    void limitsDuckDuckGoHtmlToEightResults() {
        CozeClient cozeClient = mock(CozeClient.class);
        BingRssSearchClient fallback = mock(BingRssSearchClient.class);
        SearchServiceImpl service = service(cozeClient, fallback);
        StringBuilder html = new StringBuilder("<html><body>");
        for (int index = 1; index <= 10; index++) {
            html.append("<div class=\"result\"><a class=\"result__a\" href=\"https://example.com/")
                    .append(index).append("\">Result ").append(index)
                    .append("</a><a class=\"result__snippet\">Snippet ").append(index)
                    .append("</a></div>");
        }
        html.append("</body></html>");
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.just(html.toString()));

        StepVerifier.create(service.search("query", null))
                .assertNext(results -> {
                    assertThat(results).hasSize(8);
                    assertThat(results).extracting("url")
                            .containsExactly(
                                    "https://example.com/1", "https://example.com/2",
                                    "https://example.com/3", "https://example.com/4",
                                    "https://example.com/5", "https://example.com/6",
                                    "https://example.com/7", "https://example.com/8");
                })
                .verifyComplete();
        verifyNoInteractions(fallback);
    }

    @Test
    void acceptsRawRssAndLimitsResultsToEight() {
        CozeClient cozeClient = mock(CozeClient.class);
        BingRssSearchClient fallback = mock(BingRssSearchClient.class);
        SearchServiceImpl service = service(cozeClient, fallback);
        StringBuilder items = new StringBuilder();
        for (int index = 1; index <= 10; index++) {
            items.append("<item><title>Result ").append(index)
                    .append("</title><link>https://example.com/").append(index)
                    .append("</link></item>");
        }
        String rss = " \n\uFEFF <rss version=\"2.0\"><channel>" + items + "</channel></rss>";
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.just(rss));

        StepVerifier.create(service.search("query", null))
                .assertNext(results -> {
                    assertThat(results).hasSize(8);
                    assertThat(results).extracting("url")
                            .containsExactly(
                                    "https://example.com/1", "https://example.com/2",
                                    "https://example.com/3", "https://example.com/4",
                                    "https://example.com/5", "https://example.com/6",
                                    "https://example.com/7", "https://example.com/8");
                })
                .verifyComplete();
        verifyNoInteractions(fallback);
    }

    @Test
    void rejectsRssDoctypeAndFallsBackWithoutResolvingEntities() {
        CozeClient cozeClient = mock(CozeClient.class);
        BingRssSearchClient fallback = mock(BingRssSearchClient.class);
        SearchServiceImpl service = service(cozeClient, fallback);
        String rss = """
                <?xml version="1.0"?>
                <!DOCTYPE rss [<!ENTITY xxe SYSTEM "file:///not-allowed">]>
                <rss version="2.0"><channel><item>
                  <title>&xxe;</title><link>https://example.com/unsafe</link>
                </item></channel></rss>
                """;
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.just(rss));
        com.newtech.note.entity.search.WebPage page = new com.newtech.note.entity.search.WebPage();
        page.setUrl("https://fallback.example/result");
        when(fallback.search("query")).thenReturn(Mono.just(List.of(page)));

        StepVerifier.create(service.search("query", null))
                .assertNext(results -> assertThat(results).extracting("url")
                        .containsExactly("https://fallback.example/result"))
                .verifyComplete();
    }

    @Test
    void fallsBackToPublicSearchWhenCozeFails() {
        for (String code : List.of("COZE_TIMEOUT", "COZE_4028")) {
            CozeClient cozeClient = mock(CozeClient.class);
            BingRssSearchClient fallback = mock(BingRssSearchClient.class);
            SearchServiceImpl service = service(cozeClient, fallback);
            when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                    .thenReturn(Mono.error(new BusinessException(code, "provider unavailable")));
            com.newtech.note.entity.search.WebPage page = new com.newtech.note.entity.search.WebPage();
            page.setName("Fallback");
            page.setUrl("https://fallback.example/result");
            when(fallback.search("query")).thenReturn(Mono.just(List.of(page)));

            StepVerifier.create(service.search("query", null))
                    .assertNext(results -> assertThat(results).extracting("url")
                            .containsExactly("https://fallback.example/result"))
                    .verifyComplete();
        }
    }

    @Test
    void returnsExplicitProviderErrorWhenBothSearchProvidersFail() {
        CozeClient cozeClient = mock(CozeClient.class);
        BingRssSearchClient fallback = mock(BingRssSearchClient.class);
        SearchServiceImpl service = service(cozeClient, fallback);
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.just("not-json"));
        when(fallback.search("query")).thenReturn(Mono.error(
                new BusinessException("PUBLIC_SEARCH_UNAVAILABLE", "offline")));

        StepVerifier.create(service.search("query", null))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) error).getCode())
                            .isEqualTo("WEB_SEARCH_PROVIDERS_UNAVAILABLE");
                })
                .verify();
    }

    @Test
    void neverCompletingCozeCallTimesOutAndTriggersBing() {
        CozeClient cozeClient = new CozeClient(
                WebClient.builder().exchangeFunction(request -> Mono.never()).build(),
                mock(PointsService.class), mock(UserService.class),
                "key", "https://coze.test/workflow", true, 1000, 1);
        BingRssSearchClient fallback = mock(BingRssSearchClient.class);
        SearchServiceImpl service = service(cozeClient, fallback);
        ReflectionTestUtils.setField(service, "workflowId", "workflow");
        com.newtech.note.entity.search.WebPage page = new com.newtech.note.entity.search.WebPage();
        page.setUrl("https://fallback.example/result");
        when(fallback.search("query")).thenReturn(Mono.just(List.of(page)));

        StepVerifier.create(service.search("query", MockServerHttpRequest.get("/")
                        .header(HttpHeaders.AUTHORIZATION, "Guest guest-a")
                        .header("Device-Id", "guest-a").build()))
                .assertNext(results -> assertThat(results).extracting("url")
                        .containsExactly("https://fallback.example/result"))
                .verifyComplete();
    }

    @Test
    void fallsBackToTruncatedNoteTextWhenAiKeywordExtractionFails() {
        DifyClient difyClient = mock(DifyClient.class);
        SearchServiceImpl service = new SearchServiceImpl(new ObjectMapper(), difyClient,
                mock(DeepSeekClient.class), mock(CozeClient.class), mock(BingRssSearchClient.class));
        ReflectionTestUtils.setField(service, "apiKeyForSearchKeywords", "");
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), anyString(), nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.error(DeepSeekProviderException.transportFailure(
                        new RuntimeException("providers down"))));

        StepVerifier.create(service.searchKeywords("成都火锅 宽窄巷子 旅行计划", null))
                .assertNext(keywords -> assertThat(keywords)
                        .containsExactly("成都火锅", "宽窄巷子", "旅行计划"))
                .verifyComplete();
    }

    @Test
    void doesNotFallbackForMissingCozeConfigurationOrBusinessFailures() {
        for (String code : List.of(
                "COZE_NOT_CONFIGURED", "UNAUTHORIZED", "3333", "POINTS_UNAVAILABLE",
                "COZE_HTTP_400", "COZE_HTTP_401", "COZE_HTTP_403",
                "COZE_4001", "COZE_WORKFLOW_INVALID")) {
            CozeClient cozeClient = mock(CozeClient.class);
            BingRssSearchClient fallback = mock(BingRssSearchClient.class);
            SearchServiceImpl service = service(cozeClient, fallback);
            when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                    .thenReturn(Mono.error(new BusinessException(code, "blocked")));

            StepVerifier.create(service.search("query", null))
                    .expectErrorSatisfies(error -> assertThat(((BusinessException) error).getCode())
                            .isEqualTo(code))
                    .verify();
            verifyNoInteractions(fallback);
        }
    }

    @Test
    void localConfigurationCanFallbackForUnpublishedOrUnauthorizedWorkflow() {
        for (String code : List.of("COZE_NOT_CONFIGURED", "COZE_WORKFLOW_MISSING",
                "COZE_4101", "COZE_6031")) {
            CozeClient cozeClient = mock(CozeClient.class);
            BingRssSearchClient fallback = mock(BingRssSearchClient.class);
            SearchServiceImpl service = service(cozeClient, fallback);
            ReflectionTestUtils.setField(service, "fallbackOnCozeConfigurationError", true);
            when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                    .thenReturn(Mono.error(new BusinessException(code, "workflow unavailable")));
            com.newtech.note.entity.search.WebPage page = new com.newtech.note.entity.search.WebPage();
            page.setUrl("https://fallback.example/result");
            when(fallback.search("query")).thenReturn(Mono.just(List.of(page)));

            StepVerifier.create(service.search("query", null))
                    .assertNext(results -> assertThat(results).extracting("url")
                            .containsExactly("https://fallback.example/result"))
                    .verifyComplete();
        }
    }

    @Test
    void bingFallbackIsLimitedToSelectedTransientHttpStatuses() {
        for (String code : List.of("COZE_HTTP_429", "COZE_HTTP_500", "COZE_HTTP_502",
                "COZE_HTTP_503", "COZE_HTTP_504")) {
            CozeClient cozeClient = mock(CozeClient.class);
            BingRssSearchClient fallback = mock(BingRssSearchClient.class);
            SearchServiceImpl service = service(cozeClient, fallback);
            when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                    .thenReturn(Mono.error(new BusinessException(code, "transient")));
            com.newtech.note.entity.search.WebPage page = new com.newtech.note.entity.search.WebPage();
            page.setUrl("https://fallback.example/result");
            when(fallback.search("query")).thenReturn(Mono.just(List.of(page)));

            StepVerifier.create(service.search("query", null))
                    .expectNextCount(1)
                    .verifyComplete();
        }
    }

    @Test
    void keywordFallbackDoesNotHidePointsOrProgrammingErrors() {
        for (RuntimeException failure : List.of(
                new BusinessException("POINTS_ZERO", "no points"),
                new IllegalStateException("programming error"))) {
            DifyClient difyClient = mock(DifyClient.class);
            SearchServiceImpl service = new SearchServiceImpl(new ObjectMapper(), difyClient,
                    mock(DeepSeekClient.class), mock(CozeClient.class), mock(BingRssSearchClient.class));
            when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                    any(), any(), any(), any(), any())).thenReturn(Mono.error(failure));

            StepVerifier.create(service.searchKeywords("不应退化", null))
                    .expectErrorMatches(error -> error == failure)
                    .verify();
        }
    }

    private SearchServiceImpl service(CozeClient cozeClient, BingRssSearchClient fallback) {
        return new SearchServiceImpl(new ObjectMapper(), mock(DifyClient.class),
                mock(DeepSeekClient.class), cozeClient, fallback);
    }
}
