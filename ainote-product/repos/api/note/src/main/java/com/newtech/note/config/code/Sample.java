package com.newtech.note.config.code;

import com.aliyun.tea.TeaException;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teautil.models.RuntimeOptions;
import com.newtech.note.common.CodeTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Aliyun SMS verification-code sender. */
@Slf4j
@Component
public class Sample {
    private static final String LOGIN_TEMPLATE = "SMS_477890073";
    private static final String RESET_TEMPLATE = "SMS_477935091";
    private static final String SUCCESS_CODE = "OK";

    private final String accessKeyId;
    private final String accessKeySecret;

    public Sample(
            @Value("${aliyun.sms.access-key-id:${ALIYUN_SMS_ACCESS_KEY_ID:}}") String accessKeyId,
            @Value("${aliyun.sms.access-key-secret:${ALIYUN_SMS_ACCESS_KEY_SECRET:}}")
            String accessKeySecret) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
    }

    com.aliyun.dysmsapi20170525.Client createClient(
            String configuredAccessKeyId, String configuredAccessKeySecret) throws Exception {
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                .setAccessKeyId(configuredAccessKeyId)
                .setAccessKeySecret(configuredAccessKeySecret);
        config.endpoint = "dysmsapi.aliyuncs.com";
        return new com.aliyun.dysmsapi20170525.Client(config);
    }

    /** Returns true only when Aliyun explicitly acknowledges a successful send. */
    public boolean sendingCode(String phone, String verificationCode, CodeTypeEnum type) {
        if (StringUtils.isAnyBlank(accessKeyId, accessKeySecret)) {
            log.warn("Aliyun SMS is not configured");
            return false;
        }

        try {
            com.aliyun.dysmsapi20170525.Client client = createClient(accessKeyId, accessKeySecret);
            SendSmsRequest request = new SendSmsRequest()
                    .setSignName("AI备忘录")
                    .setPhoneNumbers(phone)
                    .setTemplateParam("{\"code\":\"" + verificationCode + "\"}")
                    .setTemplateCode(type == CodeTypeEnum.LOGIN
                            ? LOGIN_TEMPLATE : RESET_TEMPLATE);
            SendSmsResponse response = client.sendSmsWithOptions(request, new RuntimeOptions());
            return response != null
                    && Integer.valueOf(200).equals(response.getStatusCode())
                    && response.getBody() != null
                    && SUCCESS_CODE.equals(response.getBody().getCode());
        } catch (TeaException failure) {
            log.warn("Aliyun SMS request failed");
            return false;
        } catch (Exception failure) {
            log.warn("Aliyun SMS request failed");
            return false;
        }
    }
}
