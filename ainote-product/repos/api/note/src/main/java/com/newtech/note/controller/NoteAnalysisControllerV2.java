package com.newtech.note.controller;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.entity.bo.OrganizedNoteBo;
import com.newtech.note.entity.bo.ProductBo;
import com.newtech.note.entity.dto.noteRelatedInfo.CategorizedNote;
import com.newtech.note.entity.dto.noteRelatedInfo.RelatedLink;
import com.newtech.note.entity.dto.noteRelatedInfo.RelatedTitle;
import com.newtech.note.entity.request.AnalyzeNoteRequestV2;
import com.newtech.note.entity.request.AnalyzeTextContentRequest;
import com.newtech.note.entity.request.AutoAnalyzeRequest;
import com.newtech.note.entity.request.ValidateNoteRequest;
import com.newtech.note.entity.vo.ValidateNoteResult;
import com.newtech.note.entity.vo.RelatedNoteResult;
import com.newtech.note.service.NoteAnalysisServiceV2;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/v2/note/analysis")
@Tag(name = "笔记分析处理器", description = """
          笔记分析处理器，当一个note被创建后，需要对其进行分析，并将其分类、关联、归档等等处理。
          大致步骤为：
              判断备忘录语义，是否具有意义
              1.如果有意义，则创建一个note_analysis_record，之后再进行AI分析处理，并把分析结果更新到record集合里。
              2. 如果没有意义，则不进行AI分析处理，也不生成note_analysis_record记录。
        """)

public class NoteAnalysisControllerV2 {

    private final NoteAnalysisServiceV2 noteAnalysisService;

    public NoteAnalysisControllerV2(NoteAnalysisServiceV2 noteAnalysisService) {
        this.noteAnalysisService = noteAnalysisService;
    }

    @Operation(summary = "自动整理", description = "通过dify对用户编写的note进行整理，" +
            "根据用户的输入的笔记id，对note进行自动整理，接受请求后直接返回，不等待分析结果\" +\n" +
            "            \" 自动整理：根据用户的输入，对note进行自动整理\" +\n" +
            "            \"1. 校验备忘录是否有意义，如果没有意义则显示相应的消息，如果有意义则创建一个note_analysis_record后，进行note的分析处理\" +\n" +
            "            \"2. 整理note正文，通过dify对用户编写的note进行整理\" +\n" +
            "            \"3. 生成AI建议，根据整理后的正文生成AI建议，注意：需要等organizeNote接口执行完成后，才能获得建议")
    @ResponseBody
    @PostMapping(value = "/autoAnalysis")
    public Mono<NoteBaseResponse<Boolean>> autoAnalysis(@RequestBody AutoAnalyzeRequest request, ServerHttpRequest req) {
        return noteAnalysisService.autoAnalysis(request, req);
    }

    @Operation(summary = "检查备忘录是否有意义，如果没有意义则显示相应的消息，如果有意义则创建一个note_analysis_record后，进行note的分析处理", description = "通过dify对用户编写的note进行整理")
    @ResponseBody
    @PostMapping(value = "/validateNote")
    public Mono<NoteBaseResponse<ValidateNoteResult>> validateNote(@RequestBody ValidateNoteRequest request, ServerHttpRequest req) {
        return noteAnalysisService.validateNote(request, req);
    }

    @Operation(summary = "整理note正文", description = "通过dify对用户编写的note进行整理")
    @ResponseBody
    @PostMapping(value = "/organizeNote")
    public Mono<NoteBaseResponse<OrganizedNoteBo>> organizeNote(@RequestBody AnalyzeNoteRequestV2 request, ServerHttpRequest req) {
        return noteAnalysisService.organizedNote(request, req);
    }

    @Operation(summary = "获得AI建议", description = "根据整理后的正文生成AI建议, " +
            "注意：需要等organizeNote接口执行完成后，才能获得建议")
    @ResponseBody
    @PostMapping(value = "/aiSuggestion", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> aiSuggestion(@RequestBody AnalyzeNoteRequestV2 request, ServerHttpRequest req) {
        return noteAnalysisService.aiSuggestion(request, req);
    }

    @Operation(summary = "猜你想看-相关标题", description = "用户点击返回的标题则调用relatedInfo接口，该接口返回标题对应的详细信息，注意：需要等organizeNote接口执行完成后，才能获得标题信息")
    @ResponseBody
    @PostMapping(value = "/relatedTitle")
    public Mono<NoteBaseResponse<List<RelatedTitle>>> relatedTitle(@RequestBody AnalyzeTextContentRequest request, ServerHttpRequest req) {
        return noteAnalysisService.relatedTitle(request, req);
    }

    @Operation(summary = "猜你想看-相关信息", description = "输入文本内容，返回这个内容的相关信息，特别地：点击相关标题后调用此接口，返回标题对应的详细信息，达到一个跳转链接的效果")
    @ResponseBody
    @PostMapping(value = "/relatedInfo")
    public Mono<NoteBaseResponse<String>> relatedInfo(@Schema(description = "文本内容") @RequestParam String textContent, ServerHttpRequest req) {
        return noteAnalysisService.relatedInfo(textContent, req);
    }


    @Operation(summary = "猜你想看-相关链接", description = "返回多个搜索结果，当前（2024-11-14）前端只要展示2条即可，注意：需要等organizeNote接口执行完成后，才能获得相关链接信息")
    @ResponseBody
    @PostMapping(value = "/relatedLink")
    public Mono<NoteBaseResponse<List<RelatedLink>>> relatedLink(@RequestBody AnalyzeTextContentRequest request, ServerHttpRequest req) {
        return noteAnalysisService.relatedLink(request, req);
    }

    @Operation(summary = "信息分类", description = "通过大模型对note进行分类，注意一个备忘录可以有多个分类，并返回多个分类信息，" +
            "这些分类信息不仅仅保存到note_analysis_records中，还会作为系统维度的笔记中的一个模块保存到notes集合里")
    @ResponseBody
    @PostMapping(value = "/categorizedNote")
    public Mono<NoteBaseResponse<List<CategorizedNote>>> categorizedNote(@RequestBody AnalyzeNoteRequestV2 request, ServerHttpRequest req) {
        return noteAnalysisService.categorizedNote(request, req);
    }

    @Operation(summary = "获得AI配图", description = "输入文本内容，返回这个内容的配图链接地址")
    @ResponseBody
    @PostMapping(value = "/aiIllustration")
    public Mono<NoteBaseResponse<String>> aiIllustration(@RequestBody AnalyzeTextContentRequest request, ServerHttpRequest req) {
        return noteAnalysisService.aiIllustration(request, req);
    }


    @Operation(summary = "获得商品推荐", description = "通过coze对note进行分析，并返回相关的商品推荐")
    @ResponseBody
    @PostMapping(value = "/productRecommendations")
    public Mono<NoteBaseResponse<List<ProductBo>>> productRecommendations(@RequestBody AnalyzeNoteRequestV2 request, ServerHttpRequest req) {
        return noteAnalysisService.productRecommendations(request, req);
    }

    @Operation(summary = "获得语义相关的备忘录")
    @ResponseBody
    @PostMapping(value = "/relatedNotes")
    public Mono<NoteBaseResponse<List<RelatedNoteResult>>> relatedNotes(
            @RequestBody AnalyzeNoteRequestV2 request, ServerHttpRequest req) {
        return noteAnalysisService.relatedNotes(request, req);
    }

}
