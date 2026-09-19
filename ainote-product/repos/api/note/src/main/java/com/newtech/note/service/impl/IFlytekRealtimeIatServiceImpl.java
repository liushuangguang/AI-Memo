package com.newtech.note.service.impl;

import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 语音听写流式 WebAPI 接口调用示例 接口文档（必看）：https://doc.xfyun.cn/rest_api/语音听写（流式版）.html
 * webapi
 * 听写服务参考帖子（必看）：http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=38947&extra=
 * 语音听写流式WebAPI
 * 服务，热词使用方式：登陆开放平台https://www.xfyun.cn/后，找到控制台--我的应用---语音听写---个性化热词，上传热词
 * 注意：热词只能在识别的时候会增加热词的识别权重，需要注意的是增加相应词条的识别率，但并不是绝对的，具体效果以您测试为准。
 * 错误码链接：https://www.xfyun.cn/document/error-code （code返回错误码时必看）
 * 语音听写流式WebAPI
 * 服务，方言或小语种试用方法：登陆开放平台https://www.xfyun.cn/后，在控制台--语音听写（流式）--方言/语种处添加
 * 添加后会显示该方言/语种的参数值
 * 
 * @author iflytek
 */

@Slf4j
public class IFlytekRealtimeIatServiceImpl extends WebSocketListener {
    private static final String hostUrl = "https://iat-api.xfyun.cn/v2/iat"; // 中英文，http url 不支持解析 ws/wss schema
    // private static final String hostUrl =
    // "https://iat-niche-api.xfyun.cn/v2/iat";//小语种
    private final String appId;
    private final String apiSecret;
    private final String apiKey;

    public IFlytekRealtimeIatServiceImpl() {
        this(
                requiredConfiguredValue("speech.app_id", "IFLYTEK_APP_ID"),
                requiredConfiguredValue("speech.api_secret", "IFLYTEK_API_SECRET"),
                requiredConfiguredValue("speech.api_key", "IFLYTEK_API_KEY"));
    }

    IFlytekRealtimeIatServiceImpl(String appId, String apiSecret, String apiKey) {
        this.appId = appId;
        this.apiSecret = apiSecret;
        this.apiKey = apiKey;
    }

    static String requiredConfiguredValue(String systemProperty, String environmentVariable) {
        String value = System.getProperty(systemProperty);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentVariable);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "IFlytek credential is not configured; set " + environmentVariable);
        }
        return value.trim();
    }
    private static final String file = "note/src/main/resources/16k_10.pcm"; // 中文
    public static final int StatusFirstFrame = 0;
    public static final int StatusContinueFrame = 1;
    public static final int StatusLastFrame = 2;
    public static final JSONObject json = JSONUtil.createObj();
    Decoder decoder = new Decoder();
    // 开始时间
    private static Date dateBegin = new Date();
    // 结束时间
    private static Date dateEnd = new Date();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");

    public void onOpen(WebSocket webSocket, Response response) {
        super.onOpen(webSocket, response);
        new Thread(() -> {
            // 连接成功，开始发送数据
            int frameSize = 1280; // 每一帧音频的大小,建议每 40ms 发送 122B
            int interval = 40;
            int status = 0; // 音频的状态
            try (FileInputStream fs = new FileInputStream(file)) {
                byte[] buffer = new byte[frameSize];
                // 发送音频
                end: while (true) {
                    int len = fs.read(buffer);
                    if (len == -1) {
                        status = StatusLastFrame; // 文件读完，改变status 为 2
                    }
                    switch (status) {
                        case StatusFirstFrame: // 第一帧音频status = 0
                            JSONObject frame = JSONUtil.createObj();
                            JSONObject business = JSONUtil.createObj(); // 第一帧必须发送
                            JSONObject common = JSONUtil.createObj(); // 第一帧必须发送
                            JSONObject data = JSONUtil.createObj(); // 每一帧都要发送
                            // 填充common
                            common.set("app_id", appId);
                            // 填充business
                            business.set("language", "zh_cn")
                                    // business.set("language", "en_us");//英文
                                    // business.set("language", "ja_jp");//日语，在控制台可添加试用或购买
                                    // business.set("language", "ko_kr");//韩语，在控制台可添加试用或购买
                                    // business.set("language", "ru-ru");//俄语，在控制台可添加试用或购买
                                    .set("domain", "iat")
                                    .set("accent", "mandarin")// 中文方言请在控制台添加试用，添加后即展示相应参数值
                                    // business.set("nunum", 0);
                                    // business.set("ptt", 0);//标点符号
                                    // business.set("rlang", "zh-hk"); // zh-cn :简体中文（默认值）zh-hk
                                    // :繁体香港(若未授权不生效，在控制台可免费开通)
                                    // business.set("vinfo", 1);
                                    .set("dwa", "wpgs");// 动态修正(若未授权不生效，在控制台可免费开通)
                            // business.set("nbest", 5);// 句子多候选(若未授权不生效，在控制台可免费开通)
                            // business.set("wbest", 3);// 词级多候选(若未授权不生效，在控制台可免费开通)
                            // 填充data
                            data.set("status", StatusFirstFrame)
                                    .set("format", "audio/L16;rate=16000")
                                    .set("encoding", "raw")
                                    .set("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)));
                            // 填充frame
                            frame.set("common", common)
                                    .set("business", business)
                                    .set("data", data);
                            webSocket.send(frame.toString());
                            status = StatusContinueFrame; // 发送完第一帧改变status 为 1
                            break;
                        case StatusContinueFrame: // 中间帧status = 1
                            JSONObject frame1 = JSONUtil.createObj();
                            JSONObject data1 = JSONUtil.createObj()
                                    .set("status", StatusContinueFrame)
                                    .set("format", "audio/L16;rate=16000")
                                    .set("encoding", "raw");
                            data1.set("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)));
                            frame1.set("data", data1);
                            webSocket.send(frame1.toString());
                            // System.out.println("send continue");
                            break;
                        case StatusLastFrame: // 最后一帧音频status = 2 ，标志音频发送结束
                            JSONObject frame2 = JSONUtil.createObj();
                            JSONObject data2 = JSONUtil.createObj();
                            data2.set("status", StatusLastFrame)
                                    .set("audio", "")
                                    .set("format", "audio/L16;rate=16000")
                                    .set("encoding", "raw");
                            frame2.set("data", data2);
                            webSocket.send(frame2.toString());
                                        log.debug("IFlytek IAT sent final audio frame");
                            break end;
                    }
                    Thread.sleep(interval); // 模拟音频采样延时
                }
                log.debug("IFlytek IAT finished sending audio frames");
            } catch (FileNotFoundException ignored) {
                log.warn("IFlytek IAT audio input file was not found");
            } catch (IOException ignored) {
                log.warn("Unable to read IFlytek IAT audio input");
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                log.warn("IFlytek IAT audio sender was interrupted");
            }
        }).start();
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        super.onMessage(webSocket, text);
        ResponseData responseData = JSONUtil.toBean(JSONUtil.parseObj(text), ResponseData.class);

        if (responseData == null) {
            log.warn("IFlytek IAT returned an empty response");
            return;
        }
        if (responseData.getCode() != 0) {
            log.warn("IFlytek IAT returned error code {}", responseData.getCode());
            return;
        }
        if (responseData.getData() == null) {
            return;
        }

        if (responseData.getData().getResult() != null) {
            try {
                decoder.decode(responseData.getData().getResult().getText());
            } catch (Exception ignored) {
                log.warn("Unable to decode IFlytek IAT response");
            }
        }
        if (responseData.getData().getStatus() == 2) {
            log.info("IFlytek IAT session completed");
            decoder.discard();
            webSocket.close(1000, "");
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable throwable, Response response) {
        super.onFailure(webSocket, throwable, response);
        if (response == null) {
            log.warn("IFlytek IAT WebSocket connection failed");
        } else {
            log.warn("IFlytek IAT WebSocket connection failed with status {}", response.code());
        }
    }

    public static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        StringBuilder builder = new StringBuilder("host: ").append(url.getHost()).append("\n").//
                append("date: ").append(date).append("\n").//
                append("GET ").append(url.getPath()).append(" HTTP/1.1");
        // System.out.println(builder);
        Charset charset = Charset.forName("UTF-8");
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(builder.toString().getBytes(charset));
        String sha = Base64.getEncoder().encodeToString(hexDigits);

        // System.out.println(sha);
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                apiKey, "hmac-sha256", "host date request-line", sha);
        // System.out.println(authorization);
        HttpUrl httpUrl = HttpUrl.parse("https://" + url.getHost() + url.getPath()).newBuilder().//
                addQueryParameter("authorization", Base64.getEncoder().encodeToString(authorization.getBytes(charset))).//
                addQueryParameter("date", date).//
                addQueryParameter("host", url.getHost()).//
                build();
        return httpUrl.toString();
    }

    @lombok.Data
    public static class ResponseData {
        private int code;
        private String message;
        private String sid;
        private Data data;
    }

    @lombok.Data
    public static class Data {
        private int status;
        private Result result;
    }

    @lombok.Data
    public static class Result {
        int bg;
        int ed;
        String pgs;
        int[] rg;
        int sn;
        Ws[] ws;
        boolean ls;
        JSONObject vad;

        public Text getText() {
            Text text = new Text();
            StringBuilder sb = new StringBuilder();
            for (Ws ws : this.ws) {
                sb.append(ws.cw[0].w);
            }
            text.sn = this.sn;
            text.text = sb.toString();
            text.sn = this.sn;
            text.rg = this.rg;
            text.pgs = this.pgs;
            text.bg = this.bg;
            text.ed = this.ed;
            text.ls = this.ls;
            text.vad = this.vad == null ? null : this.vad;
            return text;
        }
    }

    @lombok.Data
    public static class Ws {
        Cw[] cw;
        int bg;
        int ed;
    }

    @lombok.Data
    public static class Cw {
        int sc;
        String w;
    }

    @lombok.Data
    public static class Text {
        int sn;
        int bg;
        int ed;
        String text;
        String pgs;
        int[] rg;
        boolean deleted;
        boolean ls;
        JSONObject vad;
    }

    // 解析返回数据，仅供参考
    public static class Decoder {
        private Text[] texts;
        private int defc = 10;

        public Decoder() {
            this.texts = new Text[this.defc];
        }

        public synchronized void decode(Text text) {
            if (text.sn >= this.defc) {
                this.resize();
            }
            if ("rpl".equals(text.pgs)) {
                for (int i = text.rg[0]; i <= text.rg[1]; i++) {
                    this.texts[i].deleted = true;
                }
            }
            this.texts[text.sn] = text;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Text t : this.texts) {
                if (t != null && !t.deleted) {
                    sb.append(t.text);
                }
            }
            return sb.toString();
        }

        public void resize() {
            int oc = this.defc;
            this.defc <<= 1;
            Text[] old = this.texts;
            this.texts = new Text[this.defc];
            for (int i = 0; i < oc; i++) {
                this.texts[i] = old[i];
            }
        }

        public void discard() {
            for (int i = 0; i < this.texts.length; i++) {
                this.texts[i] = null;
            }
        }
    }
}
