package com.newtech.note.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.gson.Gson;
import com.newtech.note.service.IFlytekRealtimeTtsService;

import okhttp3.HttpUrl;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class IFlytekRealtimeTtsServiceImpl implements IFlytekRealtimeTtsService {
    // 地址与鉴权信息
    private final String hostUrl;
    private final String appId;
    private final String apiSecret;
    private final String apiKey;
    // 合成文本编码格式
    public static final String TTE = "UTF8"; // 小语种必须使用UNICODE编码作为值
    // 发音人参数。到控制台-我的应用-语音合成-添加试用或购买发音人，添加后即显示该发音人参数值，若试用未添加的发音人会报错11200
    public static final String VCN = "aisbabyxu";
    // 合成文件名称
    public static final String OUTPUT_FILE_PATH = "src/main/resources/tts/" + System.currentTimeMillis() + ".mp3";
    // json
    public static final Gson gson = new Gson();

    public IFlytekRealtimeTtsServiceImpl(
            @Value("${speech.tts_url:https://tts-api.xfyun.cn/v2/tts}") String hostUrl,
            @Value("${speech.app_id:}") String appId,
            @Value("${speech.api_secret:}") String apiSecret,
            @Value("${speech.api_key:}") String apiKey) {
        this.hostUrl = hostUrl;
        this.appId = appId;
        this.apiSecret = apiSecret;
        this.apiKey = apiKey;
    }

    private void requireCredentials() {
        if (!StringUtils.hasText(appId)) {
            throw new IllegalStateException(
                    "IFlytek app ID is not configured; set IFLYTEK_APP_ID");
        }
        if (!StringUtils.hasText(apiSecret)) {
            throw new IllegalStateException(
                    "IFlytek API secret is not configured; set IFLYTEK_API_SECRET");
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException(
                    "IFlytek API key is not configured; set IFLYTEK_API_KEY");
        }
    }

    @Override
    public Mono<ResponseEntity<DataBuffer>> convert(String text) throws RuntimeException, InterruptedException {
        requireCredentials();

        // 获取授权地址
        String authUrl;
        try {
            authUrl = getAuthUrl(hostUrl, apiKey, apiSecret).replace("https://", "wss://");
        } catch (Exception e) {
            throw new RuntimeException("获取讯飞授权地址失败");
        }

        Mono<DataBuffer> MonoDataBuffer = websocketWork(authUrl, text).subscribeOn(Schedulers.boundedElastic());

        return MonoDataBuffer.flatMap(fb -> Mono.just(ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "resource.mp3" + "\"")
                .body(fb)));
    }

    // Websocket方法
    /**
     * 
     * @param authUrl 授权地址
     * @param buffer  返回音频写入的缓冲区
     * @param text    输入的文本
     */
    private Mono<DataBuffer> websocketWork(String authUrl, String text) {
        // 创建 dataBuffer
        DataBufferFactory bufferFactory = new DefaultDataBufferFactory();
        DataBuffer buffer = bufferFactory.allocateBuffer(0);

        try {
            URI uri = new URI(authUrl);
            Object lock = new Object();
            WebSocketClient webSocketClient = new WebSocketClient(uri) {

                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    log.info("建立 WebSocket 连接");
                }

                @Override
                public void onMessage(String response) {
                    JsonParse myJsonParse = gson.fromJson(response, JsonParse.class);

                    if (myJsonParse.code != 0) {
                        log.warn("IFlytek TTS returned error code {}", myJsonParse.code);
                    } else if (myJsonParse.data != null) {
                        try {
                            byte[] textBase64Decode = Base64.getDecoder().decode(myJsonParse.data.audio);
                            buffer.write(textBase64Decode);
                        } catch (Exception ignored) {
                            log.warn("Unable to decode IFlytek TTS audio payload");
                        }
                        if (myJsonParse.data.status == 2) {
                            Float readableKiloByteCount = (float) buffer.readableByteCount() / 1024;
                            log.info("IFlytek TTS audio received, size: {} KiB",
                                    String.format("%.2f", readableKiloByteCount));
                            synchronized (lock) {
                                lock.notify();
                            }
                        }
                    }
                }

                @Override
                public void onClose(int i, String s, boolean b) {
                    log.info("关闭 WebSocket 连接");
                    synchronized (lock) {
                        lock.notify();
                    }
                }

                @Override
                public void onError(Exception e) {
                    log.error("IFlytek TTS WebSocket error");
                    synchronized (lock) {
                        lock.notify();
                    }
                }
            };

            /**
             * Runnable 有 bug
             * 
             * @see https://github.com/reactor/reactor-core/issues/3269
             */
            return Mono.fromCallable(new ApiThread(webSocketClient, text, lock)).flatMap(a -> Mono.just(buffer));
        } catch (Exception ignored) {
            log.warn("Unable to initialize IFlytek TTS WebSocket");
            return Mono.error(new IllegalStateException(
                    "Unable to initialize IFlytek TTS WebSocket"));
        } finally {
            // 释放缓存防止溢出
            DataBufferUtils.release(buffer);
        }

    }

    // 线程来发送音频与参数
    private class ApiThread implements Callable<Integer> {
        WebSocketClient webSocketClient;
        // 需要转换的语音
        String text;
        Object lock;

        public ApiThread(WebSocketClient webSocketClient, String text, Object lock) throws InterruptedException {
            // 建立连接
            webSocketClient.connect();
            while (!webSocketClient.getReadyState().equals(WebSocket.READYSTATE.OPEN)) {
                // log.info("正在连接...");
                Thread.sleep(100);
            }

            this.webSocketClient = webSocketClient;
            this.text = text;
            this.lock = lock;
        }

        public Integer call() {

            String requestJson;// 请求参数json串
            try {
                requestJson = "{\n" +
                        "  \"common\": {\n" +
                        "    \"app_id\": \"" + appId + "\"\n" +
                        "  },\n" +
                        "  \"business\": {\n" +
                        "    \"aue\": \"lame\",\n" +
                        "    \"tte\": \"" + TTE + "\",\n" +
                        "    \"ent\": \"intp65\",\n" +
                        "    \"vcn\": \"" + VCN + "\",\n" +
                        "    \"pitch\": 50,\n" +
                        "    \"speed\": 50\n" +
                        "  },\n" +
                        "  \"data\": {\n" +
                        "    \"status\": 2,\n" +
                        "    \"text\": \"" + Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8))
                        + "\"\n" +
                        // " \"text\": \"" +
                        // Base64.getEncoder().encodeToString(TEXT.getBytes("UTF-16LE")) + "\"\n" +
                        "  }\n" +
                        "}";
                webSocketClient.send(requestJson);
                // 等待服务端返回完毕后关闭
                synchronized (lock) {
                    lock.wait();
                }
                return 0;
            } catch (Exception ignored) {
                log.warn("IFlytek TTS request failed");
                return 1;
            } finally {
                webSocketClient.close();
            }
        }
    }

    // 鉴权方法
    private static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        // 时间
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        // 拼接
        String preStr = "host: " + url.getHost() + "\n" +
                "date: " + date + "\n" +
                "GET " + url.getPath() + " HTTP/1.1";
        // log.info(preStr);
        // SHA256加密
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(preStr.getBytes(StandardCharsets.UTF_8));
        // Base64加密
        String sha = Base64.getEncoder().encodeToString(hexDigits);
        // 拼接
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                apiKey, "hmac-sha256", "host date request-line", sha);
        // 拼接地址
        HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.parse("https://" + url.getHost() + url.getPath())).newBuilder()
                .//
                addQueryParameter("authorization",
                        Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8)))
                .//
                addQueryParameter("date", date).//
                addQueryParameter("host", url.getHost()).//
                build();

        return httpUrl.toString();
    }

    // 返回的json结果拆解
    class JsonParse {
        int code;
        String sid;
        Data data;
    }

    class Data {
        int status;
        String audio;
    }
}
