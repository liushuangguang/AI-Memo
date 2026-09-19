package com.newtech.note.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.newtech.note.service.AudioSenderService;
import com.newtech.note.util.XfUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import static com.newtech.note.service.impl.IFlytekRealtimeIatServiceImpl.*;
import static com.newtech.note.service.impl.IFlytekRealtimeTtsServiceImpl.TTE;
import static com.newtech.note.service.impl.IFlytekRealtimeTtsServiceImpl.VCN;

@Service
@Slf4j
public class AudioSenderServiceImpl implements AudioSenderService {

    @Value("${speech.app_id:}")
    private String appId;

    @Value("${speech.host_url:https://iat-api.xfyun.cn/v2/iat}")
    private String hostUrl;

    @Value("${speech.tts_url:https://tts-api.xfyun.cn/v2/tts}")
    private String ttsUrl;

    @Value("${speech.api_secret:}")
    private String apiSecret;

    @Value("${speech.api_key:}")
    private String apiKey;

    @Autowired
    private ObjectMapper objectMapper;

    private static final int frameSize = 1280; // 每帧大小
    private static final int interval = 40;

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

//    public boolean wsCloseFlag = false;// 模拟延时

//    @Autowired
//    private Sinks.Many<ByteBuffer> sinkMany;

    @Override
    public Flux<String> sendAudio(File file) {
        requireCredentials();

        return Flux.create(sink -> {
            String authUrl;
            try {
                authUrl = XfUtil.getAuthUrl(hostUrl, apiKey, apiSecret);
            } catch (Exception ignored) {
                throw new IllegalStateException("Unable to generate IFlytek authentication URL");
            }

            String url = authUrl.replace("http://", "ws://").replace("https://", "wss://");
            URI serverUri;
            try {
                serverUri = new URI(url);
            } catch (URISyntaxException e) {
                throw new IllegalStateException("Invalid IFlytek WebSocket URI");
            }

            WebSocketClient webSocketClient = new WebSocketClient(serverUri) {

                @Override
                public void onOpen(ServerHandshake handshake) {
                    new Thread(() -> {
                        //连接成功，开始发送数据
                        int status = 0;  // 音频的状态
                        try (FileInputStream fs = new FileInputStream(file)) {
                            byte[] buffer = new byte[frameSize];
                            // 发送音频
                            end:
                            while (true) {
                                int len = fs.read(buffer);
                                if (len == -1) {
                                    status = StatusLastFrame;  //文件读完，改变status 为 2
                                }
                                switch (status) {
                                    case StatusFirstFrame:   // 第一帧音频status = 0
                                        JsonObject frame = new JsonObject();
                                        JsonObject business = new JsonObject();  //第一帧必须发送
                                        JsonObject common = new JsonObject();  //第一帧必须发送
                                        JsonObject data = new JsonObject();  //每一帧都要发送
                                        // 填充common
                                        common.addProperty("app_id", appId);
                                        //填充business
                                        business.addProperty("language", "zh_cn");
                                        //business.addProperty("language", "en_us");//英文
                                        //business.addProperty("language", "ja_jp");//日语，在控制台可添加试用或购买
                                        //business.addProperty("language", "ko_kr");//韩语，在控制台可添加试用或购买
                                        //business.addProperty("language", "ru-ru");//俄语，在控制台可添加试用或购买
                                        business.addProperty("domain", "iat");
                                        business.addProperty("accent", "mandarin");//中文方言请在控制台添加试用，添加后即展示相应参数值
                                        //business.addProperty("nunum", 0);
                                        //business.addProperty("ptt", 0);//标点符号
                                        //business.addProperty("rlang", "zh-hk"); // zh-cn :简体中文（默认值）zh-hk :繁体香港(若未授权不生效，在控制台可免费开通)
                                        //business.addProperty("vinfo", 1);
                                        business.addProperty("dwa", "wpgs");//动态修正(若未授权不生效，在控制台可免费开通)
                                        //business.addProperty("nbest", 5);// 句子多候选(若未授权不生效，在控制台可免费开通)
                                        //business.addProperty("wbest", 3);// 词级多候选(若未授权不生效，在控制台可免费开通)
                                        //填充data
                                        data.addProperty("status", StatusFirstFrame);
                                        data.addProperty("format", "audio/L16;rate=16000");
                                        data.addProperty("encoding", "raw");
                                        data.addProperty("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)));
                                        //填充frame
                                        frame.add("common", common);
                                        frame.add("business", business);
                                        frame.add("data", data);
                                        this.send(frame.toString());
                                        status = StatusContinueFrame;  // 发送完第一帧改变status 为 1
                                        break;
                                    case StatusContinueFrame:  //中间帧status = 1
                                        JsonObject frame1 = new JsonObject();
                                        JsonObject data1 = new JsonObject();
                                        data1.addProperty("status", StatusContinueFrame);
                                        data1.addProperty("format", "audio/L16;rate=16000");
                                        data1.addProperty("encoding", "raw");
                                        data1.addProperty("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)));
                                        frame1.add("data", data1);
                                        this.send(frame1.toString());
                                        break;
                                    case StatusLastFrame:    // 最后一帧音频status = 2 ，标志音频发送结束
                                        JsonObject frame2 = new JsonObject();
                                        JsonObject data2 = new JsonObject();
                                        data2.addProperty("status", StatusLastFrame);
                                        data2.addProperty("audio", "");
                                        data2.addProperty("format", "audio/L16;rate=16000");
                                        data2.addProperty("encoding", "raw");
                                        frame2.add("data", data2);
                                        this.send(frame2.toString());
                                        log.info("sendlast");
                                        break end;
                                }
                                Thread.sleep(interval); //模拟音频采样延时
                            }
                            log.info("all data is send");
                        } catch (FileNotFoundException e) {
                            log.error("file not found");
                        } catch (IOException e) {
                            log.error("io exception");
                        } catch (InterruptedException e) {
                            log.error("io interrupted");
                        }
                    }).start();
                }

                @Override
                public void onMessage(String message) {
                    try {
                        ResponseData resp = objectMapper.readValue(message, ResponseData.class);
                        if (resp.getCode() != 0) {
                            log.error("IFlytek IAT returned error code {}", resp.getCode());
                            log.error("Error code reference: https://www.xfyun.cn/document/error-code");
                            return;
                        }

                        if (resp.getData() != null && resp.getData().getResult() != null) {
                            Text text = resp.getData().getResult().getText();
                            Decoder decoder = new Decoder();
                            decoder.decode(text);
                            sink.next(decoder.toString());

                            if (resp.getData().getStatus() == 2) { // 最终状态
                                sink.complete();
                                decoder.discard();
                                close();
                            }
                        }
                    } catch (JsonProcessingException e) {
                        log.error("Unable to parse IFlytek IAT response");
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.warn("IFlytek IAT WebSocket closed with status {}", code);
                    sink.complete();
                }

                @Override
                public void onError(Exception ex) {
                    log.error("IFlytek IAT WebSocket error");
                }
            };

            webSocketClient.connect();

        });
    }

    @Override
    public void receiveAudio(String text, Sinks.Many<ByteBuffer> sinkMany) {
        requireCredentials();
        String authUrl;
        try {
            authUrl = XfUtil.getAuthUrl(ttsUrl, apiKey, apiSecret);
        } catch (Exception ignored) {
            throw new IllegalStateException("Unable to generate IFlytek authentication URL");
        }

        String url = authUrl.replace("http://", "ws://").replace("https://", "wss://");
        URI serverUri;
        try {
            serverUri = new URI(url);
        } catch (URISyntaxException ignored) {
            throw new IllegalStateException("Invalid IFlytek WebSocket URI");
        }

        WebSocketClient webSocketClient = new WebSocketClient(serverUri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                new Thread(() -> {
                    String requestJson;//请求参数json串
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
                                "  }\n" +
                                "}";
                        send(requestJson);
                    } catch (Exception e) {
                        log.warn("Unable to send IFlytek TTS request");
                    }
                }).start();
            }

            @Override
            public void onMessage(String text) {
                JsonParse myJsonParse = null;
                try {
                    myJsonParse = objectMapper.readValue(text, JsonParse.class);
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException("Unable to parse IFlytek TTS response");
                }
                if (myJsonParse.code != 0) {
                    log.error("发生错误，错误码为：{}", myJsonParse.code);
                }
                if (myJsonParse.data != null) {
                    try {
                        byte[] textBase64Decode = Base64.getDecoder().decode(myJsonParse.data.audio);
                        sinkMany.tryEmitNext(ByteBuffer.wrap(textBase64Decode));
                    } catch (Exception e) {
                        log.warn("ws传输错误...");
                    }
                    if (myJsonParse.data.status == 2) {
                        sinkMany.tryEmitComplete();
                    }
                }
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                log.warn("IFlytek TTS WebSocket closed with status {}", i);
                sinkMany.tryEmitComplete();
            }

            @Override
            public void onError(Exception e) {
                log.error("IFlytek TTS WebSocket error");
            }
        };

        webSocketClient.connect();
    }

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
            if (oc >= 0) System.arraycopy(old, 0, this.texts, 0, oc);
        }
        public void discard(){
            Arrays.fill(this.texts, null);
        }
    }
}

//返回的json结果拆解
@Data
class JsonParse {
    int code;
    String sid;
    AudioData data;
}

@Data
class AudioData {
    int status;
    String audio;
}
