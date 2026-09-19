package com.newtech.note.client;

/**
 * Signals that Dify is unavailable or not configured. Services may safely
 * route these failures to another configured AI provider.
 */
public final class DifyProviderException extends RuntimeException {
    private DifyProviderException(String message) {
        super(message);
    }

    private DifyProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public static DifyProviderException notConfigured() {
        return new DifyProviderException("Dify application API key is not configured");
    }

    public static DifyProviderException httpFailure(int status) {
        return new DifyProviderException("Dify request failed with HTTP " + status);
    }

    public static DifyProviderException workflowFailure() {
        return new DifyProviderException("Dify workflow did not complete successfully");
    }

    public static DifyProviderException missingOutput(String outputField) {
        return new DifyProviderException("Dify workflow response is missing output field: " + outputField);
    }

    public static DifyProviderException transportFailure(Throwable cause) {
        return new DifyProviderException("Dify request could not be completed", cause);
    }
}
