package com.newtech.note.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IFlytekCredentialTest {

    @Test
    void iatReportsMissingCredentialWithoutExposingAValue() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> IFlytekRealtimeIatServiceImpl.requiredConfiguredValue(
                        "test.iflytek.unset", "IFLYTEK_TEST_UNSET_CREDENTIAL"));

        assertTrue(error.getMessage().contains("IFLYTEK_TEST_UNSET_CREDENTIAL"));
    }

    @Test
    void ttsFailsBeforeCreatingAnAuthenticatedUrlWhenCredentialIsMissing() {
        IFlytekRealtimeTtsServiceImpl service = new IFlytekRealtimeTtsServiceImpl(
                "https://example.invalid/v2/tts", "", "", "");

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.convert("test text"));

        assertTrue(error.getMessage().contains("IFLYTEK_APP_ID"));
    }

    @Test
    void audioSenderFailsBeforeOpeningAWebSocketWhenCredentialIsMissing() {
        AudioSenderServiceImpl service = new AudioSenderServiceImpl();
        ReflectionTestUtils.setField(service, "appId", "");
        ReflectionTestUtils.setField(service, "apiSecret", "");
        ReflectionTestUtils.setField(service, "apiKey", "");

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.sendAudio(new File("not-used.pcm")));

        assertTrue(error.getMessage().contains("IFLYTEK_APP_ID"));
    }
}
