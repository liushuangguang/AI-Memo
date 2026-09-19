package com.newtech.note.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class NoteDeltaJsonParser {
    private static final Logger logger = LogManager.getLogger(NoteDeltaJsonParser.class);

    public static String parse(String deltaJson) {
        if (!(deltaJson.startsWith("[{") && deltaJson.endsWith("}]"))) {
            return deltaJson;
        }
        // 使用 Jackson 解析 Delta JSON
        ObjectMapper objectMapper = new ObjectMapper();
        List<JsonNode> deltaList;
        try {
            deltaList = objectMapper.readValue(deltaJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse note delta JSON");
            return deltaJson;
        }
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
                JsonNode todoNode;
                try {
                    todoNode = objectMapper.readTree(todoJson);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to parse Delta JSON for note, todoJson is : %s".formatted(todoJson));
                }
                // 提取代办内容和预计完成时间
                String content = todoNode.path("content").asText();
                String scheduledAt = todoNode.path("scheduledAt").asText();
                // 转换为 Markdown 格式
                String markdown = convertToMarkdown(content, scheduledAt);
                text.append("\n").append(markdown);
            } else if (insertNode.isObject() && insertNode.has("ai_suggestion")) {
                String aiSuggestionJson = insertNode.path("ai_suggestion").asText();
                JsonNode todoNode;
                try {
                    todoNode = objectMapper.readTree(aiSuggestionJson);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to parse Delta JSON for note, todoJson is : %s".formatted(aiSuggestionJson));
                }
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

}
