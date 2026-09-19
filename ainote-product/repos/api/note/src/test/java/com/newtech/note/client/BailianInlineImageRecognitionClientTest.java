package com.newtech.note.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BailianInlineImageRecognitionClientTest {
    @TempDir
    Path uploadRoot;

    @Test
    void readsLanHostedFileLocallyAndReturnsValidatedRecognition() throws Exception {
        Files.write(uploadRoot.resolve("compressed-shot.jpg"), new byte[]{1, 2, 3, 4});
        String resultJson = """
                {"recognized_text":"明天 10:00 开会","title":"明天开会","organized_content":"明天 10:00 开会"}
                """;
        String envelope = new ObjectMapper().writeValueAsString(Map.of(
                "choices", new Object[]{Map.of("message", Map.of("content", resultJson))}));
        WebClient client = WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(envelope)
                        .build()))
                .build();
        BailianInlineImageRecognitionClient recognition = client(client);

        StepVerifier.create(recognition.recognizeStoredImage(
                        "http://192.168.1.8:8091/upload-files/compressed-shot.jpg"))
                .assertNext(value -> {
                    assertEquals("明天 10:00 开会", value.recognizedText());
                    assertEquals("明天开会", value.title());
                    assertEquals("明天 10:00 开会", value.organizedContent());
                })
                .verifyComplete();
    }

    @Test
    void requestContainsInlineBytesInsteadOfAnImageUrlThatProviderMustFetch() throws Exception {
        BailianInlineImageRecognitionClient recognition = client(WebClient.create());
        String serialized = new ObjectMapper().writeValueAsString(
                recognition.buildRequest(new byte[]{1, 2, 3}));

        assertTrue(serialized.contains("data:image/jpeg;base64,AQID"));
        assertFalse(serialized.contains("192.168."));
        assertFalse(serialized.contains("localhost"));
    }

    @Test
    void rejectsProviderOutputWithoutActuallyRecognizedText() {
        BailianInlineImageRecognitionClient recognition = client(WebClient.create());
        String envelope = """
                {"choices":[{"message":{"content":"{\\"recognized_text\\":\\"\\",\\"title\\":\\"猜测标题\\",\\"organized_content\\":\\"猜测内容\\"}"}}]}
                """;

        assertThrows(AiProviderOutputException.class, () -> recognition.parseResponse(envelope));
    }

    private BailianInlineImageRecognitionClient client(WebClient webClient) {
        return new BailianInlineImageRecognitionClient(
                webClient,
                new ObjectMapper(),
                "test-key",
                "qwen-vl-plus",
                "https://example.invalid/chat/completions",
                uploadRoot);
    }
}
