package com.newtech.note.client;

import reactor.core.publisher.Mono;

public interface ImageRecognitionClient {
    Mono<ImageRecognitionResult> recognizeStoredImage(String uploadedImageUrl);

    default Mono<ImageRecognitionResult> recognizeImageBytes(byte[] imageBytes, String mimeType) {
        return Mono.error(new UnsupportedOperationException("Inline image recognition is unavailable"));
    }

    record ImageRecognitionResult(String recognizedText, String title, String organizedContent) {
    }
}
