package com.newtech.note.client;

/** Signals a configuration, transport or protocol failure from DeepSeek. */
public final class DeepSeekProviderException extends RuntimeException {
    private DeepSeekProviderException(String message) {
        super(message);
    }

    private DeepSeekProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public static DeepSeekProviderException notConfigured() {
        return new DeepSeekProviderException(
                "DeepSeek API key is not configured; set DEEPSEEK_API_KEY on the backend");
    }

    public static DeepSeekProviderException httpFailure(int status) {
        return new DeepSeekProviderException("DeepSeek request failed with HTTP " + status);
    }

    public static DeepSeekProviderException invalidResponse() {
        return new DeepSeekProviderException("DeepSeek returned an invalid chat-completions response");
    }

    public static DeepSeekProviderException invalidUsage() {
        return new DeepSeekProviderException("DeepSeek response is missing valid token usage");
    }

    public static DeepSeekProviderException transportFailure(Throwable cause) {
        return new DeepSeekProviderException("DeepSeek request could not be completed", cause);
    }
}
