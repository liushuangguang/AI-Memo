package com.newtech.note.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtech.note.client.BingRssSearchClient;
import com.newtech.note.client.CozeClient;
import com.newtech.note.client.DeepSeekClient;
import com.newtech.note.client.DeepSeekCompletion;
import com.newtech.note.client.DifyClient;
import com.newtech.note.client.SiliconFlowEmbeddingClient;
import com.newtech.note.common.BusinessException;
import com.newtech.note.controller.NoteAnalysisControllerV2;
import com.newtech.note.entity.dto.Note;
import com.newtech.note.entity.dto.NoteAnalysisRecord;
import com.newtech.note.entity.search.WebPage;
import com.newtech.note.security.NoteOwnershipService;
import com.newtech.note.service.DifyNoteService;
import com.newtech.note.service.ImageGenService;
import com.newtech.note.service.MilvusService;
import com.newtech.note.service.NoteAnalysisRecordService;
import com.newtech.note.service.NoteServiceV2;
import com.newtech.note.service.RecommendProductService;
import com.newtech.note.service.impl.NoteCategorizeService;
import com.newtech.note.service.impl.NoteAnalysisServiceImplV2;
import com.newtech.note.service.impl.SearchServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NoteGlobalExceptionHandlerWebTest {

    @Test
    void relatedNotesMalformedModelResponseIsHttp503() {
        DeepSeekClient deepSeekClient = mock(DeepSeekClient.class);
        when(deepSeekClient.completeJsonWithUsage(anyString(), anyString()))
                .thenReturn(Mono.just(new DeepSeekCompletion("{\"matches\":\"invalid\"}", 10)));
        WebTestClient client = relatedNotesClient(deepSeekClient, 5);

        client.post().uri("/v2/note/analysis/relatedNotes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"recordId\":\"record\"}")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody(String.class).isEqualTo("相关备忘录分析暂不可用");
    }

    @Test
    void relatedNotesTotalTimeoutIsHttp504() {
        DeepSeekClient deepSeekClient = mock(DeepSeekClient.class);
        when(deepSeekClient.completeJsonWithUsage(anyString(), anyString()))
                .thenReturn(Mono.never());
        WebTestClient client = relatedNotesClient(deepSeekClient, 1);

        client.post().uri("/v2/note/analysis/relatedNotes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"recordId\":\"record\"}")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.GATEWAY_TIMEOUT)
                .expectBody(String.class).isEqualTo("Related-note analysis timed out");
    }

    @Test
    void malformedInputRemainsHttp400() {
        WebTestClient client = relatedNotesClient(mock(DeepSeekClient.class), 5);

        client.post().uri("/v2/note/analysis/relatedNotes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void unsafeBingRedirectIsHttp502() {
        BingRssSearchClient bingClient = bingClientReturning(
                ClientResponse.create(HttpStatus.FOUND)
                        .header("Location", "http://169.254.169.254/latest/meta-data")
                        .build());
        WebTestClient client = searchClient(bingClient);

        client.get().uri("/probe/search?query=test")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
                .expectBody(String.class)
                .isEqualTo("Public web search returned an unsafe redirect");
    }

    @Test
    void bingRedirectLoopRemainsHttp502AfterCozeFallback() {
        BingRssSearchClient bingClient = bingClientReturning(
                ClientResponse.create(HttpStatus.FOUND)
                        .header("Location", "/search?format=rss&q=test")
                        .build());
        WebTestClient client = searchClient(bingClient);

        client.get().uri("/probe/search?query=test")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
                .expectBody(String.class)
                .isEqualTo("Public web search returned a redirect loop");
    }

    @Test
    void bingHttpFailureRemainsHttp502AfterCozeFallback() {
        BingRssSearchClient bingClient = bingClientReturning(
                ClientResponse.create(HttpStatus.BAD_GATEWAY).build());
        WebTestClient client = searchClient(bingClient);

        client.get().uri("/probe/search?query=test")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
                .expectBody(String.class)
                .isEqualTo("Public web search request failed");
    }

    @Test
    void bothSearchProvidersUnavailableIsHttp503() {
        BingRssSearchClient bingClient = bingClientReturning(
                ClientResponse.create(HttpStatus.OK)
                        .body("not valid XML")
                        .build());
        WebTestClient client = searchClient(bingClient);

        client.get().uri(uriBuilder -> uriBuilder.path("/probe/search")
                        .queryParam("query", "test").build())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody(String.class)
                .isEqualTo("Coze and public web search providers are unavailable");
    }

    @Test
    void unexpectedFailureUsesFixedHttp500Body() {
        WebTestClient client = webClient(new UnexpectedFailureProbeController());

        client.get().uri("/probe/unexpected")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(String.class).isEqualTo("An unexpected error occurred");
    }

    @Test
    void unknownBusinessExceptionUsesFixedHttp500BodyWithoutMessageLeak() {
        WebTestClient client = webClient(new UnknownBusinessFailureProbeController());

        client.get().uri("/probe/business-unknown")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(String.class).isEqualTo("An unexpected error occurred");
    }

    @Test
    void dynamicPublicSearchHttpPrefixIsNotPublicAndDoesNotLeakMessage() {
        WebTestClient client = webClient(new DynamicBusinessFailureProbeController());

        client.get().uri("/probe/business-dynamic")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(String.class).isEqualTo("An unexpected error occurred");
    }

    @Test
    void whitelistedBusinessCodeUsesFixedBodyInsteadOfExceptionMessage() {
        WebTestClient client = webClient(new KnownBusinessFailureProbeController());

        client.get().uri("/probe/business-known")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
                .expectBody(String.class)
                .isEqualTo("Public web search returned an unsafe redirect");
    }

    @Test
    void nonExceptionThrowableUsesFixedHttp500BodyWithoutMessageLeak() {
        WebTestClient client = webClient(new ThrowableFailureProbeController());

        client.get().uri("/probe/throwable")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(String.class).isEqualTo("An unexpected error occurred");
    }

    private WebTestClient relatedNotesClient(DeepSeekClient deepSeekClient, long timeoutSeconds) {
        NoteServiceV2 noteService = mock(NoteServiceV2.class);
        NoteOwnershipService ownershipService = mock(NoteOwnershipService.class);
        NoteAnalysisRecord record = new NoteAnalysisRecord();
        record.setId("record");
        record.setNoteId("source");
        record.setRawNote("analysis snapshot");
        Note source = note("source", "guest-a", "current source", "current source body");
        Note candidate = note("candidate", "guest-a", "candidate", "candidate body");
        when(ownershipService.ownedAnalysisRecord(anyString(), any(ServerHttpRequest.class)))
                .thenReturn(Mono.just(record));
        when(ownershipService.ownedNote(anyString(), any(ServerHttpRequest.class)))
                .thenReturn(Mono.just(source));
        when(noteService.listRelatedNoteCandidates("guest-a", "source"))
                .thenReturn(Flux.just(candidate));
        NoteAnalysisServiceImplV2 service = new NoteAnalysisServiceImplV2(
                noteService,
                mock(DifyNoteService.class),
                mock(ImageGenService.class),
                mock(NoteAnalysisRecordService.class),
                mock(NoteCategorizeService.class),
                mock(RecommendProductService.class),
                mock(MilvusService.class),
                mock(SiliconFlowEmbeddingClient.class),
                ownershipService,
                deepSeekClient,
                new ObjectMapper(),
                timeoutSeconds);
        return webClient(new NoteAnalysisControllerV2(service));
    }

    private Note note(String id, String owner, String title, String content) {
        Note note = new Note();
        note.setId(id);
        note.setDeviceId(owner);
        note.setTitle(title);
        note.setContent(content);
        return note;
    }

    private BingRssSearchClient bingClientReturning(ClientResponse response) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(response))
                .build();
        return new BingRssSearchClient(webClient, "https://www.bing.com/search", 2, 8);
    }

    private WebTestClient searchClient(BingRssSearchClient bingClient) {
        CozeClient cozeClient = mock(CozeClient.class);
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.error(new BusinessException("COZE_TIMEOUT", "coze timeout")));
        SearchServiceImpl searchService = new SearchServiceImpl(
                new ObjectMapper(), mock(DifyClient.class), mock(DeepSeekClient.class),
                cozeClient, bingClient);
        return webClient(new SearchProbeController(searchService));
    }

    private WebTestClient webClient(Object controller) {
        return WebTestClient.bindToController(controller)
                .controllerAdvice(new NoteGlobalExceptionHandler())
                .configureClient()
                .responseTimeout(Duration.ofSeconds(4))
                .build();
    }

    @RestController
    private static class SearchProbeController {
        private final SearchServiceImpl searchService;

        private SearchProbeController(SearchServiceImpl searchService) {
            this.searchService = searchService;
        }

        @GetMapping("/probe/search")
        Mono<List<WebPage>> search(@RequestParam String query, ServerHttpRequest request) {
            return searchService.search(query, request);
        }
    }

    @RestController
    private static class UnexpectedFailureProbeController {
        @GetMapping("/probe/unexpected")
        Mono<String> fail() {
            return Mono.error(new IllegalStateException("sensitive internal detail"));
        }
    }

    @RestController
    private static class UnknownBusinessFailureProbeController {
        @GetMapping("/probe/business-unknown")
        Mono<String> fail() {
            return Mono.error(new BusinessException(
                    "UNLISTED_NOT_FOUND", "sensitive provider and database detail"));
        }
    }

    @RestController
    private static class DynamicBusinessFailureProbeController {
        @GetMapping("/probe/business-dynamic")
        Mono<String> fail() {
            return Mono.error(new BusinessException(
                    "PUBLIC_SEARCH_HTTP_599", "sensitive upstream marker"));
        }
    }

    @RestController
    private static class KnownBusinessFailureProbeController {
        @GetMapping("/probe/business-known")
        Mono<String> fail() {
            return Mono.error(new BusinessException(
                    "PUBLIC_SEARCH_REDIRECT_INVALID", "sensitive allowlisted marker"));
        }
    }

    @RestController
    private static class ThrowableFailureProbeController {
        @GetMapping("/probe/throwable")
        Mono<String> fail() {
            return Mono.error(new SensitiveThrowable("sensitive throwable marker"));
        }
    }

    private static final class SensitiveThrowable extends Throwable {
        private SensitiveThrowable(String message) {
            super(message);
        }
    }
}
