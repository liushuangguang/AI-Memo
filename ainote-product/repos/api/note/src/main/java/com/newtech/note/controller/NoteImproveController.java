package com.newtech.note.controller;

import com.newtech.note.entity.dto.CompleteInfo;
import com.newtech.note.entity.request.NoteToImproveRequest;
import com.newtech.note.service.DifyNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/note/improve")
@Tag(name = "笔记完善处理器", description = """
          笔记完善处理器，用户在备忘录记录/的信息：
          1）若存在信息不明确的词/句。
          需要你找到源备忘录中描述不准确或含义模糊的词/句，向用户提问，并给出最多4个选项让用户选择以明确词/句的含义
          2)若不存在信息不明确的词/句。
          不返回
        """)
public class NoteImproveController {
    @Autowired
    DifyNoteService difyNoteService;


    @Operation(summary = "ai信息完善", description = "通过dify对用户编写的note，描述不准确或含义模糊的词/句进行完善")
    @ResponseBody
    @PostMapping(value = "/completeInfo")
    public Mono<List<CompleteInfo>> aiCompleteInfo(@RequestBody NoteToImproveRequest request, ServerHttpRequest req) {
        return difyNoteService.completeInfo(request.getContent(), req);
    }
}
