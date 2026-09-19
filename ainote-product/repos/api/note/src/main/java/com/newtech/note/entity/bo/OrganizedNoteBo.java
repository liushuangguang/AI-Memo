package com.newtech.note.entity.bo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Transient;

import java.util.List;
import java.util.Optional;

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
@Setter
@Getter
@NoArgsConstructor
@Schema(description = "整理后的备忘录")
public class OrganizedNoteBo {
    @Schema(description = "备忘录标题")
    private String title;
    @Schema(description = "用户场景")
    private String userScenario;
    @Schema(description = "备忘录内容")
    private String bodyText;
    @Schema(description = "备忘录分类")
    private String memoClassification;
    @Schema(description = "待办列表")
    private List<TodoBo> todoList;
    @Schema(description = "标签")
    private List<String> tag;


    @JsonIgnore
    @Transient
    public String getContentText() {
        StringBuilder contentBuilder = new StringBuilder();
        Optional.ofNullable(title).ifPresent(contentBuilder::append);
        Optional.ofNullable(userScenario).ifPresent(contentBuilder::append);
        Optional.ofNullable(bodyText).ifPresent(contentBuilder::append);
        contentBuilder.append("\n");
        if (todoList != null) {
            todoList.forEach(item -> {
                if (item != null) {
                    contentBuilder.append(item).append("\n");
                }
            });
        }
        return contentBuilder.toString();
    }

}
