package com.newtech.note.service;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.entity.bo.RewrittenContentBo;
import com.newtech.note.entity.request.ProvideAssistantDirectionRequest;
import com.newtech.note.entity.request.RewriteNoteRequest;
import com.newtech.note.entity.request.ValidateNoteRequest;
import com.newtech.note.entity.vo.ProvideAssistantDirection;
import com.newtech.note.entity.vo.ValidateNoteResult;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

public interface NoteAssistService {

    /**
     * 检查备忘录是否有意义
     *
     * @param request 校验备忘录请求
     * @return 校验结果
     */
    Mono<NoteBaseResponse<ValidateNoteResult>> validateNote(ValidateNoteRequest request, ServerHttpRequest req);

    Mono<NoteBaseResponse<RewrittenContentBo>> rewriteContent(RewriteNoteRequest request, ServerHttpRequest req);

    Mono<NoteBaseResponse<ProvideAssistantDirection>> provideAssistantDirection(ProvideAssistantDirectionRequest request, ServerHttpRequest req);
}
