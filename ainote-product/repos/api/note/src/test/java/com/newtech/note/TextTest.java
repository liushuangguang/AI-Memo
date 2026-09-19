package com.newtech.note;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;

public class TextTest {
    public static void main(String[] args) {
        /*String str = "信息归类1 个入规划(17）\n" +
                "·归类原因：用户表达旅游和商业计划的个人规划。\n" +
                "·目标描述。游并洽谈 200 万元的项目。\n" +
                "·目标类型:职业\n" +
                "·截止日期:未提供具体日期\n" +
                "·行动计划:计划未详细说明，但包含旅游和商务洽谈\n" +
                "·记录时间\n" +
                "\n" +
                "信息归类 财务管理(15)\n" +
                ".归类原因:用户提到了公资金的进账情况。\n" +
                ".类型:收入\n" +
                "·金额:1516\n" +
                ".交易描述:-资金进账，用于购买抗癌药物。\n" +
                "·记录时间 2024-08-07 18:06:03\n" +
                "\n" +
                "信息归类 3:健康相关\n" +
                ".归类原因:用户提到购买抗癌药物的计划。\n" +
                ".待购药物:氧每(可能的药品全称为氧氟沙星片或氧氟沙星胶囊)1\n" +
                "·医院:未提供具体医院名称\n" +
                "·后续治疗规划:未提供详细规划";
        String[] split = str.split("\\n\\n");
        Arrays.stream(split).map(String::trim).filter(s -> s.startsWith("信息归类")).forEach(System.out::println);*/

        // String deltaJson = "[{\"insert\": \"Hello \"}, {\"insert\": \"world\", \"attributes\": {\"bold\": true}}, {\"insert\": \"\\n\"}]";

        //String deltaJson = "[{\"insert\":\"测试delta解析\\n\\n标题一\"},{\"insert\":\"\\n\",\"attributes\":{\"header\":1}},{\"insert\":\"标题二\"},{\"insert\":\"\\n\",\"attributes\":{\"header\":2}},{\"insert\":\"标题三\"},{\"insert\":\"\\n\",\"attributes\":{\"header\":3}},{\"insert\":\"\\n\"},{\"insert\":\"斜体\",\"attributes\":{\"italic\":true}},{\"insert\":\"\\n\"},{\"insert\":\"下划线\",\"attributes\":{\"italic\":true,\"underline\":true}},{\"insert\":\"\\n\\n\"},{\"insert\":\"红色的\",\"attributes\":{\"italic\":true,\"underline\":true,\"color\":\"#FFF44336\"}},{\"insert\":\"\\n\\n\"},{\"insert\":\"列表1\",\"attributes\":{\"color\":\"#FF1E88E5\"}},{\"insert\":\"\\n\",\"attributes\":{\"list\":\"ordered\"}},{\"insert\":\"列表2\",\"attributes\":{\"color\":\"#FF1E88E5\"}},{\"insert\":\"\\n\",\"attributes\":{\"list\":\"ordered\"}},{\"insert\":\"\\n\"},{\"insert\":\"链接测试\",\"attributes\":{\"color\":\"#FF1E88E5\",\"link\":\"https://sogou.com\"}},{\"insert\":\"\\n\"}]";

        /*String deltaJson = "[{\"insert\":\"测试delta解析\\n\\n标题一\"},{\"insert\":\"\\n\",\"attributes\":{\"header\":1}},{\"insert\":\"标题二\"},{\"insert\":\"\\n\",\"attributes\":{\"header\":2}},{\"insert\":\"标题三\"},{\"insert\":\"\\n\",\"attributes\":{\"header\":3}},{\"insert\":\"\\n\"},{\"insert\":\"斜体\",\"attributes\":{\"italic\":true}},{\"insert\":\"\\n\"},{\"insert\":\"下划线\",\"attributes\":{\"italic\":true,\"underline\":true}},{\"insert\":\"\\n\\n\"},{\"insert\":\"红色的\",\"attributes\":{\"italic\":true,\"underline\":true,\"color\":\"#FFF44336\"}},{\"insert\":\"\\n\\n\"},{\"insert\":\"列表1\",\"attributes\":{\"color\":\"#FF1E88E5\"}},{\"insert\":\"\\n\",\"attributes\":{\"list\":\"ordered\"}},{\"insert\":\"列表2\",\"attributes\":{\"color\":\"#FF1E88E5\"}},{\"insert\":\"\\n\",\"attributes\":{\"list\":\"ordered\"}},{\"insert\":\"\\n\"},{\"insert\":\"链接测试\",\"attributes\":{\"color\":\"#FF1E88E5\",\"link\":\"https://sogou.com\"}},{\"insert\":\"\\n\\n\\n\"},{\"insert\":{\"todo\":\"{\\\"id\\\":\\\"92bfc5f7-f796-42ad-9f3c-dcf1f11a0ebc\\\",\\\"content\\\":\\\"待办测试1\\\",\\\"scheduledAt\\\":\\\"2024/11/23 10:31\\\",\\\"done\\\":false,\\\"description\\\":\\\"待办详情待办详情待办详情待办详情待办详情待办详情\\\\n待办详情待办详情\\\"}\"}},{\"insert\":\" \\n\"},{\"insert\":{\"todo\":\"{\\\"id\\\":\\\"32aca705-6992-4dba-ab9d-55499ca1f81d\\\",\\\"content\\\":\\\"待办2\\\",\\\"scheduledAt\\\":\\\"2024/11/23 10:32\\\",\\\"done\\\":false,\\\"description\\\":\\\"\\\"}\"}},{\"insert\":\" \\n\\n\"}]";

        try {
            String pureText = extractTextFromDeltaJson(deltaJson);
            System.out.println(pureText);
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        String deltaJson2 = "[{\"insert\":\"\uD83C\uDF04 周末爬山看日出\"},{\"insert\":{\"todo\":{\"id\":\"9e346623-1a71-4754-91c0-45a1985155e0\",\"content\":\"爬山看日出\",\"scheduledAt\":\"2024/12/14 05:00\",\"done\":false,\"description\":\"\",\"emoji\":null,\"extraIcons\":null,\"noIcon\":null}}},{\"insert\":{\"dashed_line\":\"\"}},{\"insert\":{\"ai_suggestion\":[{\"id\":\"e23b021e-da6a-4800-870d-899398c58be1\",\"content\":\"提前查看天气预报，确保当日适合看日出\uD83C\uDF24\",\"scheduledAt\":null,\"done\":null,\"description\":null,\"emoji\":\"\uD83C\uDF04\",\"extraIcons\":null,\"noIcon\":null},{\"id\":\"c26d1327-b4e0-4926-8754-00163e5af227\",\"content\":\"准备爬山装备\",\"scheduledAt\":\"2024/12/13 20:00\",\"done\":null,\"description\":\"包括舒适的衣物、登山鞋、手电筒和足够的水和食物\",\"emoji\":null,\"extraIcons\":null,\"noIcon\":null},{\"id\":\"49b5cb2b-5cff-4dfb-936d-c7ff642df254\",\"content\":\"查找到山的距离和预计爬山时间\",\"scheduledAt\":null,\"done\":null,\"description\":null,\"emoji\":\"⏰\",\"extraIcons\":null,\"noIcon\":null},{\"id\":\"c9a83782-7126-4a84-a164-d3960723737a\",\"content\":\"考虑带一些应急药品，如创可贴、消毒液等\",\"scheduledAt\":null,\"done\":null,\"description\":null,\"emoji\":\"\uD83C\uDFE5\",\"extraIcons\":null,\"noIcon\":null},{\"id\":\"5dfb8c6f-f321-4d2f-9237-e7a17e72eb9a\",\"content\":\"设置闹钟，确保早起\",\"scheduledAt\":\"\",\"done\":null,\"description\":\"根据路程和日出时间，合理设定闹钟时间，建议提前30分钟到达山顶\",\"emoji\":null,\"extraIcons\":null,\"noIcon\":null}]}},{\"insert\":\"\\n\"}]";
        try {
            String pureText = extractTextFromDeltaJson(deltaJson2);
            System.out.println(pureText);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static String extractTextFromDeltaJson(String deltaJson) throws IOException {
        // 使用 Jackson 解析 Delta JSON
        ObjectMapper objectMapper = new ObjectMapper();
        List<JsonNode> deltaList = objectMapper.readValue(deltaJson, new TypeReference<>() {
        });
        StringBuilder text = new StringBuilder();
        // 遍历 Delta JSON 的操作
        // 遍历列表，提取 "insert" 和 "todo"
        for (JsonNode item : deltaList) {
            // 提取 "insert" 的值
            String insertValue = item.path("insert").asText();
            if (!insertValue.isEmpty()) {
                text.append(insertValue);
            }

            // 提取 "todo" 的值
            JsonNode insertNode = item.path("insert");
            if (insertNode.isObject() && insertNode.has("todo")) {
                String todoJson = insertNode.path("todo").asText();
                JsonNode todoNode = objectMapper.readTree(todoJson);
                // 提取代办内容和预计完成时间
                String content = todoNode.path("content").asText();
                String scheduledAt = todoNode.path("scheduledAt").asText();

                // 转换为 Markdown 格式
                String markdown = convertToMarkdown(content, scheduledAt);
                text.append("\n").append(markdown);
            } else if (insertNode.isObject() && insertNode.has("ai_suggestion")) {
                String todoJson = insertNode.path("ai_suggestion").asText();
                JsonNode todoNode = objectMapper.readTree(todoJson);
                // 提取代办内容和预计完成时间
                String content = todoNode.path("content").asText();
                String description = todoNode.path("description").asText();
                String scheduledAt = todoNode.path("scheduledAt").asText();

                // 转换为 Markdown 格式
                String markdown = convertToMarkdown(content + (StringUtils.isBlank(description) ? "" : " - " + description)
                        , scheduledAt);
                text.append("\n").append(markdown);
            }
        }
        return text.toString();
    }

    // 将代办内容和预计完成时间转换为 Markdown 格式
    private static String convertToMarkdown(String content, String scheduledAt) {
        String formattedContent = String.format("### 代办事项\n- **内容**: %s", content);
        if (!StringUtils.isEmpty(scheduledAt)) {
            formattedContent += String.format("\n- **预计完成时间**: %s", scheduledAt);
        }
        return formattedContent;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Todo {
        private String id;
        private String content;
        // 保留日期为字符串
        private String scheduledAt;
        private boolean done;
        private String description;

        @Override
        public String toString() {
            return "Todo{" +
                    "id='" + id + '\'' +
                    ", content='" + content + '\'' +
                    ", scheduledAt='" + scheduledAt + '\'' +
                    ", done=" + done +
                    ", description='" + description + '\'' +
                    '}';
        }
    }


}
