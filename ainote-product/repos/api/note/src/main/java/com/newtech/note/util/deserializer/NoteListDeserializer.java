package com.newtech.note.util.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NoteListDeserializer extends JsonDeserializer<List<String>> {

    @Override
    public List<String> deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        List<String> noteList = new ArrayList<>();
        JsonNode node = p.getCodec().readTree(p);

        if (node.isArray()) {
            for (JsonNode item : node) {
                // 如果是字符串类型，直接加入列表
                if (item.isTextual()) {
                    noteList.add(item.asText());
                }
                // 如果是对象类型，提取 "note" 字段
                else if (item.isObject() && item.has("note")) {
                    noteList.add(item.get("note").asText());
                }
            }
        }
        return noteList;
    }
}