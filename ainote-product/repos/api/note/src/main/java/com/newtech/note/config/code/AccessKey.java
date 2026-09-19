package com.example.erp_project.config;

/**
 * 阿里云短信服务
 */
public class AccessKey {
    // 阿里云短信服务
    // accessKeyId
    public static final String accessKeyId = requireCredential(
            "aliyun.sms.access-key-id", "ALIYUN_SMS_ACCESS_KEY_ID");
    // accessKeySecret
    public static final String accessKeySecret = requireCredential(
            "aliyun.sms.access-key-secret", "ALIYUN_SMS_ACCESS_KEY_SECRET");
    // 短信模板code
    //账户注册
    public static final String LOGIN = "SMS_477890073";
    //密码重置
    public static final String RESET = "SMS_477935091";

    private static String requireCredential(String systemProperty, String environmentVariable) {
        String value = System.getProperty(systemProperty);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentVariable);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Aliyun SMS credential is not configured; set " + environmentVariable);
        }
        return value.trim();
    }
}
