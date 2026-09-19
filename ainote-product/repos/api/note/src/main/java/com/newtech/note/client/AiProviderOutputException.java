package com.newtech.note.client;

/** A provider completed, but its output did not satisfy the documented response contract. */
public final class AiProviderOutputException extends RuntimeException {
    public AiProviderOutputException(String message) {
        super(message);
    }

    public AiProviderOutputException(String message, Throwable cause) {
        super(message, cause);
    }
}
