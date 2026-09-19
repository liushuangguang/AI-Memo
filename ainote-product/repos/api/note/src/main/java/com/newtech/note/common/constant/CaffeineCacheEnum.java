package com.newtech.note.common.constant;

import lombok.Getter;

import java.util.concurrent.TimeUnit;

@Getter
public enum CaffeineCacheEnum {

    /**
     * 手机验证码
     */
    MOBILE_SMSCODE(10_000, 5, TimeUnit.MINUTES),

    /** Separate per-phone and per-socket-peer reservations; process-local only. */
    MOBILE_SMS_RATE_LIMIT(20_000, 60, TimeUnit.SECONDS)
    ;

    private final long maxSize;

    private final long duration;

    private final TimeUnit unit;

    CaffeineCacheEnum() {
        this(1000, 60, TimeUnit.SECONDS);
    }

    CaffeineCacheEnum(long maxSize, long duration, TimeUnit unit) {
        this.maxSize = maxSize;
        this.duration = duration;
        this.unit = unit;
    }
}
