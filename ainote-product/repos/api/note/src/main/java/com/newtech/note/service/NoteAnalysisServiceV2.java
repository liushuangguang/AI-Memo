package com.newtech.note.service;

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
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface NoteAnalysisServiceV2 {

    /**
     * 检查备忘录是否有意义
     *
     * @param request 校验备忘录请求
     * @return 校验结果
     */
    Mono<NoteBaseResponse<ValidateNoteResult>> validateNote(ValidateNoteRequest request, ServerHttpRequest req);

    /**
     * 整理备忘录，提取用户场景、主题、以及代办信息
     *
     * @param request 整理备忘录请求
     * @return 整理后的备忘录
     */
    Mono<NoteBaseResponse<OrganizedNoteBo>> organizedNote(AnalyzeNoteRequestV2 request, ServerHttpRequest req);

    /**
     * 获取AI建议，当前为流式响应，单个流元素并非结构式的，需要客户端自行解析，后续会改为结构式的响应
     *
     * @param request 获取AI建议请求
     * @return AI建议
     */
    Flux<String> aiSuggestion(AnalyzeNoteRequestV2 request, ServerHttpRequest req);

    /**
     * 获取相关信息的标题
     *
     * @param request 获取相关信息请求
     * @return 相关信息
     */
    Mono<NoteBaseResponse<List<RelatedTitle>>> relatedTitle(AnalyzeTextContentRequest request, ServerHttpRequest req);

    /**
     * 自动分析备忘录
     *
     * @param request 自动分析备忘录请求
     * @return 自动分析结果
     */
    Mono<NoteBaseResponse<Boolean>> autoAnalysis(AutoAnalyzeRequest request, ServerHttpRequest req);

    /**
     * 获取相关信息的链接
     *
     * @param request 获取相关信息请求
     * @return 相关信息
     */
    Mono<NoteBaseResponse<List<RelatedLink>>> relatedLink(AnalyzeTextContentRequest request, ServerHttpRequest req);

    /**
     * 给定备忘录，将备忘录内容进行不同分类
     *
     * @param request 分类笔记请求
     * @return 分类结果
     */
    Mono<NoteBaseResponse<List<CategorizedNote>>> categorizedNote(AnalyzeNoteRequestV2 request, ServerHttpRequest req);

    /**
     * 给定文本内容，获取相关信息
     *
     * @param textContent 给定文本内容
     * @return 文本内容的相关信息
     */
    Mono<NoteBaseResponse<String>> relatedInfo(String textContent, ServerHttpRequest req);

    /**
     * 给定备忘录或文本内容，获取AI配图地址
     *
     * @param request 给定备忘录或文本内容请求
     * @return 文本内容的相关AI配图地址
     */
    Mono<NoteBaseResponse<String>> aiIllustration(AnalyzeTextContentRequest request, ServerHttpRequest req);

    /**
     * 给定备忘录或文本内容，获取产品推荐
     *
     * @param request 给定备忘录或文本内容请求
     * @return 产品推荐
     */
    Mono<NoteBaseResponse<List<ProductBo>>> productRecommendations(AnalyzeNoteRequestV2 request, ServerHttpRequest req);

    /**
     * 给定备忘录或文本内容，获取相关备忘录
     *
     * @param request 给定备忘录或文本内容，获取相关备忘录
     * @return 相关备忘录
     */
    Mono<NoteBaseResponse<List<RelatedNoteResult>>> relatedNotes(AnalyzeNoteRequestV2 request,
                                                                 ServerHttpRequest req);

}
