package com.newtech.note.client.entity.bailian;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.newtech.note.util.deserializer.CustomLocalDateTimeDeserializer;
import com.newtech.note.util.deserializer.NoteListDeserializer;

import java.time.LocalDateTime;
import java.util.List;

public record BailianResponseData(
        @JsonProperty("wechat_img_ai_stream")
        WechatImgAIStream wechatImgAIStream
) {
    public record WechatImgAIStream(@JsonProperty("to_do_list")
                                    List<ToDoItem> todoList,
                                    @JsonProperty("note_list")
                                    @JsonDeserialize(using = NoteListDeserializer.class)
                                    List<String> noteList,
                                    @JsonProperty("chat_history")
                                    List<ChatMessage> chatHistory,
                                    @JsonProperty("note_md")
                                    String note) {

    }

    public record ToDoItem(
            String task,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
            @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)  // 自定义反序列化
            LocalDateTime time
    ) {
    }

    public record ChatMessage(
            String sender,
            String message
    ) {
    }
}
