package com.newtech.note.controller;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.entity.bo.RewrittenContentBo;
import com.newtech.note.entity.request.ProvideAssistantDirectionRequest;
import com.newtech.note.entity.request.RewriteNoteRequest;
import com.newtech.note.entity.request.ValidateNoteRequest;
import com.newtech.note.entity.vo.ProvideAssistantDirection;
import com.newtech.note.entity.vo.ValidateNoteResult;
import com.newtech.note.service.NoteAssistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/note/assist")
@Tag(name = "笔记辅助处理器", description = """
          笔记辅助处理器，当一个note被创建后，需要对其进行辅助。
          大致步骤为：
              判断备忘录语义，是否具有意义
              1.如果有意义，则创建一个note_assist_records，之后再进行AI辅助处理，并把辅助结果更新到record集合里。
              2. 如果没有意义，则不进行AI辅助处理，也不生成note_assist_records记录。
        """)

public class NoteAssistController {

    private final NoteAssistService noteAssistService;

    public NoteAssistController(NoteAssistService noteAssistService) {
        this.noteAssistService = noteAssistService;
    }


    @Operation(summary = "检查备忘录是否有意义，如果没有意义则显示相应的消息，如果有意义则创建一个note_assist_record后，进行note的辅助处理", description = "通过dify对用户编写的note进行辅助复写")
    @ResponseBody
    @PostMapping(value = "/validateNote")
    public Mono<NoteBaseResponse<ValidateNoteResult>> validateNote(@RequestBody ValidateNoteRequest request, ServerHttpRequest req) {
        return noteAssistService.validateNote(request, req);
    }
    @Operation(summary = "内容辅助", description = "通过coze对note进行内容辅助，并返回辅助内容")
    @ResponseBody
    @RequestMapping(value = "/rewriteContent", method = RequestMethod.POST)
    public Mono<NoteBaseResponse<RewrittenContentBo>> rewriteContent(@RequestBody RewriteNoteRequest request, ServerHttpRequest req) {
        return noteAssistService.rewriteContent(request, req);
    }

    @Operation(summary = "获得选中的内容辅助方向", description = "通过coze对note进行内容辅助，并返回辅助内容")
    @ResponseBody
    @PostMapping("/selectedAssistantDirection")
    public Mono<NoteBaseResponse<ProvideAssistantDirection>> selectedAssistantDirection(
            @RequestBody ProvideAssistantDirectionRequest request, ServerHttpRequest req) {
        return noteAssistService.provideAssistantDirection(request, req);
    }
}
