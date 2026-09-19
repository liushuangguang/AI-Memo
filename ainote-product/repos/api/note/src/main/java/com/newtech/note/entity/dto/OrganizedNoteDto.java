package com.newtech.note.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.newtech.note.entity.bo.OrganizedNoteBo;
import lombok.Data;

import java.util.List;

/*
"title": "明天去吃火锅",
  "user_scenario": "计划明天与他人一起去吃火锅，作为一次用餐安排",
  "body_text": "📅 明天去吃火锅",
  "Memo_classification": "日常购物",
  "todo": [
    {
      "description": "去吃火锅",
      "time": "2024-11-02"
    }
  ],
  "tag": ["火锅", "聚餐", "用餐安排"]
 */
@Data
public class OrganizedNoteDto {
    private String title;
    @JsonProperty("user_scenario")
    private String userScenario;
    @JsonProperty("body_text")
    private String bodyText;
    @JsonProperty("Memo_classification")
    private String memoClassification;
    @JsonProperty("todo")
    private List<TodoDto> todoList;
    private List<String> tag;

    public OrganizedNoteBo transferToBo() {
        OrganizedNoteBo bo = new OrganizedNoteBo();
        bo.setTitle(this.getTitle());
        bo.setUserScenario(this.getUserScenario());
        bo.setBodyText(this.getBodyText());
        bo.setMemoClassification(this.getMemoClassification());
        bo.setTodoList(TodoDto.transferListToBoList(this.getTodoList()));
        bo.setTag(this.getTag());
        return bo;
    }
}
