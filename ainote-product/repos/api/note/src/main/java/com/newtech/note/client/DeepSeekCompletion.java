package com.newtech.note.client;

/** A DeepSeek response together with its original text and provider-reported token usage. */
public record DeepSeekCompletion(String content, String rawContent, long totalTokens) {
    /** Keeps existing callers on sanitized content while retaining strict-parser compatibility. */
    public DeepSeekCompletion(String content, long totalTokens) {
        this(content, content, totalTokens);
    }
}
