package com.newtech.note.controller;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.entity.dto.NoteAnalysis;
import com.newtech.note.entity.request.AnalyzeNoteRequest;
import com.newtech.note.entity.request.AssistContentRequest;
import com.newtech.note.entity.request.SelectAssistanceDirectionRequest;
import com.newtech.note.entity.vo.CategorizedNote;
import com.newtech.note.entity.vo.NoteProductRecommendation;
import com.newtech.note.entity.vo.NoteRelatedLink;
import com.newtech.note.entity.vo.RelationalInformationVo;
import com.newtech.note.service.NoteAnalysisService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Hidden
@RestController
@RequestMapping("/note/analysis")
@Tag(name = "笔记分析处理器-old", description = """
        笔记分析处理器，当一个note被创建后，需要对其进行分析，并将其分类、关联、归档等等处理。这5个都是关于一键整理的接口，需要传入一个版本号version，后端的大致逻辑就是我会判断这个版本的数据是否存在，如果存在我就直接返回，不存在则调用coze 的agent再返回（返回之前保存到数据库）
        关于版本号怎么传的问题：
        每次一键整理、内容辅助都需要进行调用/note/analysis/history/create接口，这个接口会返回一个version，这个version会作为参数传入到其他接口中。""")
public class NoteAnalysisController {

    private final NoteAnalysisService noteAnalysisService;

    public NoteAnalysisController(NoteAnalysisService noteAnalysisService) {
        this.noteAnalysisService = noteAnalysisService;
    }

    @Operation(summary = "整理note正文", description = "通过coze对用户编写的note进行整理")
    @ResponseBody
    @PostMapping(value = "/organizedNoteText", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> organizedNoteText(@RequestBody AnalyzeNoteRequest request, ServerHttpRequest req) {
        return noteAnalysisService.organizedNoteText(request, req);
    }

    @Operation(summary = "获得相关文档", description = "通过coze对文档打标签，并返回相同标签的文档，按照相关性倒序排列返回")
    @ResponseBody
    @PostMapping(value = "/relatedNotes")
    public Mono<NoteBaseResponse<List<NoteAnalysis>>> relatedNotes(@RequestBody AnalyzeNoteRequest request, ServerHttpRequest req) {
        return noteAnalysisService.relatedNotes(request, req);
    }

    @Operation(summary = "获得同类型笔记", description = "通过coze对文档进行归类，并返回相同分类的文档，比如返回所有的密码笔记")
    @ResponseBody
    @PostMapping(value = "/categorizedNotes")
    public Mono<NoteBaseResponse<List<CategorizedNote>>> categorizedNotes(@RequestBody AnalyzeNoteRequest request, ServerHttpRequest req) {
        return noteAnalysisService.categorizedNotes(request, req);
    }

    @Operation(summary = "获得商品推荐", description = "通过coze对note进行分析，并返回相关的商品推荐")
    @ResponseBody
    @PostMapping(value = "/productRecommendations")
    public Mono<NoteBaseResponse<List<NoteProductRecommendation>>> productRecommendations(@RequestBody AnalyzeNoteRequest request, ServerHttpRequest req) {
        return noteAnalysisService.productRecommendations(request, req);
    }

    @Operation(summary = "获得相关链接", description = "通过coze对note进行分析，并返回获得相关链接")
    @ResponseBody
    @PostMapping(value = "/relatedLinks")
    public Mono<NoteBaseResponse<List<NoteRelatedLink>>> relatedLinks(@RequestBody AnalyzeNoteRequest request, ServerHttpRequest req) {
        return noteAnalysisService.relatedLinks(request, req);
    }

    @Operation(summary = "获得-猜你想看/相关信息内容", description = "通过coze对note生成猜你想看内容，返回json+md内容")
    @ResponseBody
    @PostMapping("/relationalInformation")
    public Mono<NoteBaseResponse<RelationalInformationVo>> relationalInformation(@RequestBody AnalyzeNoteRequest request, ServerHttpRequest req) {
        return noteAnalysisService.relationalInformation(request, req);
    }

    @Operation(summary = "内容辅助", description = "通过coze对note进行内容辅助，并返回辅助内容")
    @ResponseBody
    @RequestMapping(value = "/assistedContent", method = RequestMethod.POST, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> assistedContent(@RequestBody AssistContentRequest request, ServerHttpRequest req) {
        return noteAnalysisService.assistedContent(request, req);
    }

    @Operation(summary = "获得选中的内容辅助方向", description = "通过coze对note进行内容辅助，并返回辅助内容")
    @ResponseBody
    @PostMapping("/selectedAssistanceDirection")
    public Mono<NoteBaseResponse<List<String>>> selectedAssistanceDirection(
            @RequestBody SelectAssistanceDirectionRequest request, ServerHttpRequest req) {
        return noteAnalysisService.selectedAssistanceDirection(request, req);
    }

    @Operation(summary = "获得图片链接", description = "通过coze对note生成图片链接，并返回图片链接")
    @ResponseBody
    @PostMapping("/imageLink")
    public Mono<NoteBaseResponse<String>> imageLink(@RequestBody AnalyzeNoteRequest request, ServerHttpRequest req) {
        return noteAnalysisService.imageLink(request, req);
    }

}
