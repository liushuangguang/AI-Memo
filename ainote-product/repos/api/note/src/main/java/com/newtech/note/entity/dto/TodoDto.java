package com.newtech.note.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.newtech.note.entity.bo.TodoBo;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TodoDto {
    @JsonProperty("description")
    private String content;

    @JsonProperty("time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledAt;

    public static TodoBo transferToBo(TodoDto todo) {
        TodoBo bo = new TodoBo();
        bo.setContent(todo.getContent());
        bo.setScheduledAt(todo.getScheduledAt());
        return bo;
    }

    public static List<TodoBo> transferListToBoList(List<TodoDto> todoList) {
        return todoList.stream().map(TodoDto::transferToBo).toList();
    }
}
