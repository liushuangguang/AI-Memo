package com.newtech.note.client.entity.coze;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @param content_type
 * @param data
 * @param original_result
 * @param type_for_model  example demo:
 *                        <code>
 *                        {
 *                        "content_type": 1,
 *                        "data": {
 *                        "notes": [
 *                        {
 *                        "action": "提醒 A 参加明天下午的会议",
 *                        "time": "2024-09-19:12:00"
 *                        }
 *                        ],
 *                        "todo": "A：我明天下午要去参加一个会议，你记得提醒我一下。B：好呀，没问题。",
 *                        "img_undsd": "",
 *                        "output_ocr": "21:27\n刘浩然电鸭ai算法coze\n有问题可以随时找我\n目前没啥啦,等你那个测试集,\n然后继续调试\n嗯嗯,我等下弄截图\n等在的\n21:23\n不急,你有时间时候再搞吧。\n嗯嗯\n你可以在这里添加协作者\n我进去可以补充下提取备忘录\n和待办这块的Prompt\n好,我明天加入下组织\nok"
 *                        },
 *                        "original_result": null,
 *                        "type_for_model": 2
 *                        }
 *                        </code>
 */
public record CozeImageNoteInfo(int content_type, CozeImageNoteData data, String original_result,
                                int type_for_model, int token) {
    /**
     * Data 记录
     *
     * @param notes
     * @param todo
     * @param img_undsd
     * @param output_ocr
     */
    public record CozeImageNoteData(
            List<String> notes,
            List<CozeImageTodo> todo,
            String img_undsd,
            String output_ocr
    ) {
    }

    public record CozeImageTodo(
            @JsonProperty("action")
            String action,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
            @JsonProperty("time")
            LocalDateTime time
    ) {
    }

}


