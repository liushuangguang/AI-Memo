package com.newtech.note.config.code;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.newtech.note.common.CodeTypeEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SampleTest {

    @Test
    void returnsTrueOnlyForExplicitAliyunSuccess() throws Exception {
        Client client = mock(Client.class);
        Sample sender = spy(new Sample("configured-id", "configured-secret"));
        doReturn(client).when(sender).createClient(any(), any());
        when(client.sendSmsWithOptions(any(), any()))
                .thenReturn(response(200, "OK"), response(200, "isv.BUSINESS_LIMIT_CONTROL"),
                        response(500, "OK"), null);

        assertThat(sender.sendingCode("13800000000", "654321", CodeTypeEnum.LOGIN)).isTrue();
        assertThat(sender.sendingCode("13800000000", "654321", CodeTypeEnum.LOGIN)).isFalse();
        assertThat(sender.sendingCode("13800000000", "654321", CodeTypeEnum.LOGIN)).isFalse();
        assertThat(sender.sendingCode("13800000000", "654321", CodeTypeEnum.LOGIN)).isFalse();
    }

    @Test
    void unconfiguredProviderFailsClosedBeforeClientCreation() throws Exception {
        Sample sender = spy(new Sample("", ""));

        assertThat(sender.sendingCode("13800000000", "654321", CodeTypeEnum.LOGIN)).isFalse();

        verify(sender, never()).createClient(any(), any());
    }

    @Test
    void providerExceptionFailsClosed() throws Exception {
        Client client = mock(Client.class);
        Sample sender = spy(new Sample("configured-id", "configured-secret"));
        doReturn(client).when(sender).createClient(any(), any());
        when(client.sendSmsWithOptions(any(), any())).thenThrow(new RuntimeException("provider"));

        assertThat(sender.sendingCode("13800000000", "654321", CodeTypeEnum.RESET)).isFalse();
    }

    private SendSmsResponse response(int status, String code) {
        return new SendSmsResponse()
                .setStatusCode(status)
                .setBody(new SendSmsResponseBody().setCode(code));
    }
}
