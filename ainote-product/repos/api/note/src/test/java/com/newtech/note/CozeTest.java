//package com.newtech.note;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import com.newtech.note.client.CozeClient;
//import com.newtech.note.client.entity.coze.CozeImageNoteInfo;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.web.reactive.function.client.WebClient;
//
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//
//@SpringBootTest
//public class CozeTest {
//    @Autowired
//    private WebClient webClient;
//    @Autowired
//    private ObjectMapper objectMapper;
//    @Autowired
//    private CozeClient cozeClient;
//
//    private String buildRequestBody(String message, String botId, boolean stream) {
//        Map<String, Object> params = new HashMap<>();
//        params.put("conversation_id", "123");
//        params.put("bot_id", botId);
//        params.put("user", "29032201862555");
//        params.put("auto_save_history", false);
//        // params.put("query", message);
//        params.put("stream", stream);
//        Map<String, String> messageMap = new HashMap<>();
//        messageMap.put("role", "user");
//        messageMap.put("content", message);
//        messageMap.put("content_type", "text");
//        params.put("additional_messages", messageMap);
//        String body;
//        try {
//            body = objectMapper.writeValueAsString(params);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//        return body;
//    }
//
//    //@Test
//    public void test() throws IOException {
//        String requestBody = buildRequestBody("我想去看书", "7379855594448240680", true);
//        System.out.println(requestBody);
//        webClient.post()
//                .uri("https://api.coze.cn/open_api/v2/chat")
//                // .accept(MediaType.TEXT_EVENT_STREAM)
//                .header("Authorization",
//                        "Bearer " + System.getenv("COZE_API_KEY"))
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(requestBody)
//                .retrieve()
//                .bodyToMono(String.class)
//                .log()
//                .subscribe(System.out::println);
//        System.in.read();
//    }
//
//    //@Test
//    public void testCallWorkflow() throws IOException {
//        String str = "{\"content_type\":1,\"data\":{\"notes\":[{\"note\":\"2024-09-18 12:30为当前时间，请留意任何时间相关事项。\"}],\"todo\":\"A：下周我要去上海出差，你帮我预定一下酒店吧。B：好呀，你有具体的要求吗？A：离客户公司近一点，价格适中就行。\",\"img_undsd\":\"\",\"output_ocr\":\"21:27\\n刘浩然电鸭ai算法coze\\n有问题可以随时找我\\n目前没啥啦,等你那个测试集,\\n然后继续调试\\n嗯嗯,我等下弄截图\\n等在的\\n21:23\\n不急,你有时间时候再搞吧。\\n嗯嗯\\n你可以在这里添加协作者\\n我进去可以补充下提取备忘录\\n和待办这块的Prompt\\n好,我明天加入下组织\\nok\"},\"original_result\":null,\"type_for_model\":2}";
//        cozeClient
//                .callCozeWorkflowApiFiltered(
//                        "7414412052347093003",
//                        "",
//                        Map.of("BOT_USER_INPUT", "", "image", "http://aifunc.top/upload-files/51ca7fa5-207f-49cd-8b77-bab1173d1826.pic"),
//                        Map.of())
//                .<String>handle((data, sink) -> {
//                    try {
//                        CozeImageNoteInfo cozeImageNoteInfo = objectMapper.readValue(data, CozeImageNoteInfo.class);
//                        System.out.println(cozeImageNoteInfo);
//
//                    } catch (Exception e) {
//                        sink.error(new RuntimeException(e));
//                        return;
//                    }
//                    sink.next(data);
//                }).subscribe(System.out::println);
//        System.in.read();
//    }
//
//    public static void main(String[] args) throws JsonProcessingException {
//        String str = "{\n" +
//                "  \"content_type\": 1,\n" +
//                "  \"data\": {\n" +
//                "    \"notes\": null,\n" +
//                "    \"todo\": [\n" +
//                "      {\n" +
//                "        \"action\": \"准备上次讨论项目的文件\",\n" +
//                "        \"time\": \"2024-09-25 12:54\"\n" +
//                "      }\n" +
//                "    ],\n" +
//                "    \"img_undsd\": null,\n" +
//                "    \"output_ocr\": \"21:27\\n刘浩然电鸭ai算法coze\\n有问题可以随时找我\\n目前没啥啦,等你那个测试集,\\n然后继续调试\\n嗯嗯,我等下弄截图\\n等在的\\n21:23\\n不急,你有时间时候再搞吧。\\n嗯嗯\\n你可以在这里添加协作者\\n我进去可以补充下提取备忘录\\n和待办这块的Prompt\\n好,我明天加入下组织\\nok\\n\"\n" +
//                "  },\n" +
//                "  \"original_result\": null,\n" +
//                "  \"type_for_model\": 2\n" +
//                "}\n";
//        ObjectMapper objectMapper1 = new ObjectMapper();
//        objectMapper1.registerModule(new JavaTimeModule());
//        CozeImageNoteInfo cozeImageNoteInfo = objectMapper1.readValue(str, CozeImageNoteInfo.class);
//        System.out.println(cozeImageNoteInfo);
//
//
//    }
//}
