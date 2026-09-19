
package com.newtech.note.config;


import com.newtech.note.entity.dto.UserAuthentication;

public class SecurityLocalContext {
    private SecurityLocalContext() {
    }

    /** @deprecated Reactive request authentication must never be stored in a ThreadLocal. */
    @Deprecated(forRemoval = true)
    public static void bindUserAuthentication(UserAuthentication userAuthentication) {
    }

    /** @deprecated Resolve identity from the current request instead. */
    @Deprecated(forRemoval = true)
    public static UserAuthentication getUserAuthentication() {
        return new UserAuthentication();
    }

    /** @deprecated Retained as a no-op for binary compatibility. */
    @Deprecated(forRemoval = true)
    public static void clear() {
    }
}
