package com.newtech.note.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtech.note.client.CozeClient;
import com.newtech.note.common.BusinessException;
import com.newtech.note.config.ImageFallbackStorageProperties;
import com.newtech.note.config.ResourceConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;

class ImageGenServiceImplTest {

    @Configuration
    @EnableWebFlux
    static class TestWebFluxConfiguration {
    }

    @TempDir
    Path tempDirectory;

    @Test
    void returnsImageUrlFromOutput() {
        CozeClient cozeClient = mock(CozeClient.class);
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.just("{\"output\":\" https://s.coze.cn/image.png \"}"));

        StepVerifier.create(new ImageGenServiceImpl(new ObjectMapper(), cozeClient).gen("prompt", null))
                .expectNext("https://s.coze.cn/image.png")
                .verifyComplete();
    }

    @Test
    void sendsOriginalNoteAndPrefersFirstValidImages1Url() {
        CozeClient cozeClient = mock(CozeClient.class);
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.just("""
                        {"images1":[{"url":"not-a-url"},{"url":"https://img.coze.cn/first.png"}],
                         "output":"https://img.coze.cn/legacy.png"}
                        """));

        StepVerifier.create(new ImageGenServiceImpl(new ObjectMapper(), cozeClient).gen("笔记原文", null))
                .expectNext("https://img.coze.cn/first.png")
                .verifyComplete();

        ArgumentCaptor<Map<String, Object>> parameters = ArgumentCaptor.forClass(Map.class);
        verify(cozeClient).callCozeWorkflowApiFiltered(any(), anyString(), parameters.capture(), any(), any());
        assertThat(parameters.getValue()).containsEntry("original_note", "笔记原文");
        assertThat(parameters.getValue()).doesNotContainKey("inp");
    }

    @Test
    void supportsJsonEncodedImages1Object() {
        CozeClient cozeClient = mock(CozeClient.class);
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.just("{\"images1\":\"{\\\"image\\\":{\\\"url\\\":\\\"https://img.coze.cn/a.png\\\"}}\"}"));

        StepVerifier.create(new ImageGenServiceImpl(new ObjectMapper(), cozeClient).gen("prompt", null))
                .expectNext("https://img.coze.cn/a.png")
                .verifyComplete();
    }

    @Test
    void extractsUrlFromPublishedWorkflowDataText() {
        CozeClient cozeClient = mock(CozeClient.class);
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.just("""
                        {"content_type":1,
                         "data":"【整理后的正文】\\n周末去杭州西湖散步。\\nhttps://s.coze.cn/example-image",
                         "original_result":null,
                         "type_for_model":2}
                        """));

        StepVerifier.create(new ImageGenServiceImpl(new ObjectMapper(), cozeClient).gen("笔记原文", null))
                .expectNext("https://s.coze.cn/example-image")
                .verifyComplete();
    }

    @Test
    void extractsLastValidUrlFromPublishedWorkflowDataText() {
        CozeClient cozeClient = mock(CozeClient.class);
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.just("""
                        {"data":"参考来源：https://example.com/article。\\n生成图片：https://s.coze.cn/example-image.png）。"}
                        """));

        StepVerifier.create(new ImageGenServiceImpl(new ObjectMapper(), cozeClient).gen("笔记原文", null))
                .expectNext("https://s.coze.cn/example-image.png")
                .verifyComplete();
    }

    @Test
    void skipsMalformedUrlCandidatesAndUsesLastValidUrl() {
        CozeClient cozeClient = mock(CozeClient.class);
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.just("""
                        {"data":"候选地址：https://[malformed.example/image， 最终图片：https://img.byteimg.com/final.png！"}
                        """));

        StepVerifier.create(new ImageGenServiceImpl(new ObjectMapper(), cozeClient).gen("笔记原文", null))
                .expectNext("https://img.byteimg.com/final.png")
                .verifyComplete();
    }

    @Test
    void ignoresMetadataUrlWhenKnownDataFieldHasNoImageUrl() {
        CozeClient cozeClient = mock(CozeClient.class);
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.just("""
                        {"data":"没有生成图片地址",
                         "metadata":{"url":"https://example.com/debug"}}
                        """));

        StepVerifier.create(new ImageGenServiceImpl(new ObjectMapper(), cozeClient,
                        tempDirectory, "https://fallback.test", "upload-files").gen("prompt", null))
                .expectNextMatches(url -> url.startsWith("https://fallback.test/upload-files/"))
                .verifyComplete();
    }

    @Test
    void failsForMissingOrEmptyOutput() {
        for (String response : new String[]{"{}", "{\"output\":\" \"}",
                "{\"images1\":\"javascript:alert(1)\"}"}) {
            CozeClient cozeClient = mock(CozeClient.class);
            when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                    .thenReturn(Mono.just(response));

            StepVerifier.create(new ImageGenServiceImpl(new ObjectMapper(), cozeClient,
                            tempDirectory, "https://fallback.test", "upload-files").gen("prompt", null))
                    .expectNextMatches(url -> url.startsWith("https://fallback.test/upload-files/"))
                    .verifyComplete();
        }
    }

    @Test
    void failsForNonJsonResponse() {
        CozeClient cozeClient = mock(CozeClient.class);
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.just("not-json"));

        StepVerifier.create(new ImageGenServiceImpl(new ObjectMapper(), cozeClient,
                        tempDirectory, "https://fallback.test", "upload-files").gen("prompt", null))
                .expectNextMatches(url -> url.startsWith("https://fallback.test/upload-files/"))
                .verifyComplete();
    }

    @Test
    void ignoresUnrelatedMetadataUrlsAndUsesOnlyKnownImageFields() {
        CozeClient cozeClient = mock(CozeClient.class);
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.just("""
                        {"images1":{"metadata":{"url":"https://example.com/debug"}},
                         "output":"https://img.ibytedtos.com/legacy.png"}
                        """));

        StepVerifier.create(new ImageGenServiceImpl(new ObjectMapper(), cozeClient).gen("prompt", null))
                .expectNext("https://img.ibytedtos.com/legacy.png")
                .verifyComplete();
    }

    @Test
    void fallsBackToRandomlyNamedDownloadablePngOnCoze4028() throws Exception {
        CozeClient cozeClient = mock(CozeClient.class);
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.error(new BusinessException("COZE_4028", "credits exhausted")));
        ImageGenServiceImpl service = new ImageGenServiceImpl(new ObjectMapper(), cozeClient,
                tempDirectory, "https://example.test/", "upload-files");

        String url = service.gen("private memo text", null).block();
        URI uri = URI.create(url);
        Path imagePath = tempDirectory.resolve(Path.of(uri.getPath()).getFileName().toString());
        BufferedImage image = ImageIO.read(imagePath.toFile());

        assertThat(url).startsWith("https://example.test/upload-files/");
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(640);
        assertThat(image.getHeight()).isEqualTo(360);
        String secondUrl = new ImageGenServiceImpl(new ObjectMapper(), cozeClient,
                tempDirectory, "https://example.test/", "upload-files").gen("private memo text", null).block();
        assertThat(secondUrl).isNotEqualTo(url);
        String memoDigest = java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest("ai-illustration-fallback|private memo text".getBytes(StandardCharsets.UTF_8)));
        assertThat(url).doesNotContain(memoDigest);
        assertThat(secondUrl).doesNotContain(memoDigest);
    }

    @Test
    void nonDefaultFallbackUrlIsServedFromSharedDirectoryAsPrivatePng() {
        CozeClient cozeClient = mock(CozeClient.class);
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.error(new BusinessException("COZE_4028", "credits exhausted")));
        ImageFallbackStorageProperties storage = new ImageFallbackStorageProperties(
                tempDirectory.resolve("generated-assets").toString(), "/private/generated/");
        ImageGenServiceImpl service = new ImageGenServiceImpl(new ObjectMapper(), cozeClient,
                storage, "https://fallback.test/", "s.coze.cn");

        String url = service.gen("private memo", null).block();
        assertThat(url).startsWith("https://fallback.test/private/generated/");

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(TestWebFluxConfiguration.class);
            context.registerBean(ResourceConfiguration.class, () -> new ResourceConfiguration(storage));
            context.refresh();
            WebTestClient client = WebTestClient.bindToApplicationContext(context).build();

            byte[] png = client.get()
                    .uri(URI.create(url).getRawPath())
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.IMAGE_PNG)
                    .expectHeader().value(HttpHeaders.CACHE_CONTROL,
                            value -> assertThat(value).contains("private").contains("no-store"))
                    .expectBody(byte[].class)
                    .returnResult()
                    .getResponseBody();

            assertThat(png).isNotNull().startsWith(0x89, 0x50, 0x4e, 0x47);
        }
    }

    @Test
    void sameMemoGetsDistinctCapabilityKeysAcrossConcurrentCalls() {
        CozeClient cozeClient = mock(CozeClient.class);
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.error(new BusinessException("COZE_4028", "credits exhausted")));
        ImageGenServiceImpl service = new ImageGenServiceImpl(new ObjectMapper(), cozeClient,
                tempDirectory, "https://example.test", "upload-files");

        List<String> urls = reactor.core.publisher.Flux.range(0, 12)
                .flatMap(index -> service.gen("same memo", null))
                .collectList()
                .block();

        assertThat(urls).isNotNull().hasSize(12);
        assertThat(urls).doesNotHaveDuplicates();
        assertThat(tempDirectory.toFile().listFiles()).hasSize(12);
    }

    @Test
    void rejectsUntrustedHostsAuthorityTricksAndAllIpLiteralsViaFallback() {
        String[] unsafeUrls = {
                "http://example.com/image.png",
                "https://user@example.com/image.png",
                "https://example.com/image.png#fragment",
                "https://example.com:8443/image.png",
                "https://example.com/image.png",
                "https://coze.cn.evil.example/image.png",
                "https://evilcoze.cn/image.png",
                "https://s.coze.cn.nip.io/image.png",
                "https://localhost/image.png",
                "https://asset.local/image.png",
                "https://127.0.0.1/image.png",
                "https://10.0.0.1/image.png",
                "https://100.64.0.1/image.png",
                "https://169.254.1.1/image.png",
                "https://172.16.0.1/image.png",
                "https://192.168.0.1/image.png",
                "https://192.0.2.1/image.png",
                "https://224.0.0.1/image.png",
                "https://240.0.0.1/image.png",
                "https://[::1]/image.png",
                "https://[::]/image.png",
                "https://[fc00::1]/image.png",
                "https://[fe80::1]/image.png",
                "https://[ff02::1]/image.png",
                "https://[2001:db8::1]/image.png"
        };

        for (String unsafeUrl : unsafeUrls) {
            CozeClient cozeClient = mock(CozeClient.class);
            when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                    .thenReturn(Mono.just("{\"output\":\"" + unsafeUrl + "\"}"));

            StepVerifier.create(new ImageGenServiceImpl(new ObjectMapper(), cozeClient,
                            tempDirectory, "https://fallback.test", "upload-files").gen("same memo", null))
                    .expectNextMatches(url -> url.startsWith("https://fallback.test/upload-files/"))
                    .verifyComplete();
        }
    }

    @Test
    void acceptsOnlyDefaultTrustedCozeAndVolcanoCdnSuffixes() {
        for (String safeUrl : new String[]{
                "https://s.coze.cn/public/image.png",
                "https://media.coze.cn:443/image.png",
                "https://p3.byteimg.com/image.png",
                "https://tos.volces.com/image.png",
                "https://bucket.ibytedtos.com/image.png"
        }) {
            CozeClient cozeClient = mock(CozeClient.class);
            when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                    .thenReturn(Mono.just("{\"output\":\"" + safeUrl + "\"}"));

            StepVerifier.create(new ImageGenServiceImpl(new ObjectMapper(), cozeClient).gen("prompt", null))
                    .expectNext(safeUrl)
                    .verifyComplete();
        }
    }

    @Test
    void configuredTrustedSuffixUsesLabelBoundaries() {
        CozeClient cozeClient = mock(CozeClient.class);
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.just("{\"output\":\"https://cdn.custom.example/image.png\"}"));
        ImageGenServiceImpl service = new ImageGenServiceImpl(new ObjectMapper(), cozeClient,
                tempDirectory, "https://fallback.test", "upload-files", "custom.example");

        StepVerifier.create(service.gen("prompt", null))
                .expectNext("https://cdn.custom.example/image.png")
                .verifyComplete();

        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.just("{\"output\":\"https://notcustom.example/image.png\"}"));
        StepVerifier.create(service.gen("prompt", null))
                .expectNextMatches(url -> url.startsWith("https://fallback.test/upload-files/"))
                .verifyComplete();
    }

    @Test
    void doesNotFallbackForUnrelatedErrors() {
        CozeClient cozeClient = mock(CozeClient.class);
        when(cozeClient.callCozeWorkflowApiFiltered(any(), anyString(), any(), any(), any()))
                .thenReturn(Mono.error(new BusinessException("UNAUTHORIZED", "invalid owner")));

        StepVerifier.create(new ImageGenServiceImpl(new ObjectMapper(), cozeClient,
                        tempDirectory, "https://example.test", "upload-files").gen("prompt", null))
                .expectErrorSatisfies(error -> assertThat(error).isInstanceOf(BusinessException.class)
                        .extracting(BusinessException.class::cast)
                        .extracting(BusinessException::getCode)
                        .isEqualTo("UNAUTHORIZED"))
                .verify();
        assertThat(tempDirectory.toFile().listFiles()).isNullOrEmpty();
    }
}
