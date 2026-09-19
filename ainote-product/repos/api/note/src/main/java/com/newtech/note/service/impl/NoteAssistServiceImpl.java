package com.newtech.note.service.impl;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.entity.bo.RewrittenContentBo;
import com.newtech.note.entity.dto.NoteAssistRecord;
import com.newtech.note.entity.request.*;
import com.newtech.note.entity.vo.ProvideAssistantDirection;
import com.newtech.note.entity.vo.ValidateNoteResult;
import com.newtech.note.service.DifyNoteService;
import com.newtech.note.service.NoteAssistRecordService;
import com.newtech.note.service.NoteAssistService;
import com.newtech.note.security.NoteOwnershipService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
public class NoteAssistServiceImpl implements NoteAssistService {
    private final NoteAssistRecordService noteAssistRecordService;
    private final DifyNoteService difyNoteService;

    private final NoteOwnershipService ownershipService;


    public NoteAssistServiceImpl(NoteAssistRecordService noteAssistRecordService,
                                 DifyNoteService difyNoteService,
                                 NoteOwnershipService ownershipService) {
        this.noteAssistRecordService = noteAssistRecordService;
        this.difyNoteService = difyNoteService;
        this.ownershipService = ownershipService;
    }

    @Override
    public Mono<NoteBaseResponse<ValidateNoteResult>> validateNote(ValidateNoteRequest request, ServerHttpRequest req) {
        return ownershipService.ownedNote(request.getId(), req).flatMap(note -> {
            if (StringUtils.isBlank(note.getContent())) {
                return Mono.just(NoteBaseResponse.failure("笔记内容不能为空"));
            }
            return difyNoteService.isNoteMakeSense(note.getContent(), req)
                    .flatMap(messageNumber -> {
                        NoteAnalysisServiceImplV2.RawNoteLevel level = NoteAnalysisServiceImplV2.RawNoteLevel.getLevel(messageNumber);
                        if (level == null) {
                            return Mono.just(NoteBaseResponse.failure("未知的校验结果"));
                        }
                        if (level.isNotMeaningful()) {
                            return Mono.just(NoteBaseResponse.success(ValidateNoteResult.builder().isMeaningful(false).result(level.getMessage()).build()));
                        }
                        return noteAssistRecordService.create(CreateNoteAssistRecordRequest.builder().noteId(request.getId()).build())
                                .map(NoteAssistRecord::getId).map(id -> NoteBaseResponse.success(ValidateNoteResult.builder().isMeaningful(true).recordId(id).result(level.getMessage()).build()));
                    });

        });
    }

    @Override
    public Mono<NoteBaseResponse<RewrittenContentBo>> rewriteContent(RewriteNoteRequest request, ServerHttpRequest req) {
        return ownershipService.ownedAssistRecord(request.getRecordId(), req)
                .flatMap(record -> {
                    if (record.getRewrittenContent() != null) {
                        return Mono.just(NoteBaseResponse.success(record.getRewrittenContent()));
                    }
                    return difyNoteService.rewriteContent(record.getRawNote(),
                                    StringUtils.isBlank(request.getSelectedContent()) ? record.getRawNote() : request.getSelectedContent(),
                                    String.join(" ", request.getAssistDirections()), req)
                            .flatMap(rewrittenContent -> {
                                RewrittenContentBo rewrittenContentBo = rewrittenContent.transferToBo();
                                UpdateNoteAssistRecordRequest updateHistoryRequest = new UpdateNoteAssistRecordRequest();
                                updateHistoryRequest.setId(record.getId());
                                Optional.ofNullable(request.getSelectedContent()).ifPresent(updateHistoryRequest::setSelectedContent);
                                updateHistoryRequest.setRewrittenContent(rewrittenContentBo);
                                updateHistoryRequest.setAssistDirection(request.getAssistDirections());
                                return noteAssistRecordService.update(updateHistoryRequest)
                                        .thenReturn(NoteBaseResponse.success(rewrittenContentBo));
                            });

                })
                .switchIfEmpty(Mono.just(NoteBaseResponse.failure("找不到相应辅助记录")));
    }

    @Override
    public Mono<NoteBaseResponse<ProvideAssistantDirection>> provideAssistantDirection(ProvideAssistantDirectionRequest request, ServerHttpRequest req) {
        return ownershipService.ownedAssistRecord(request.getNoteId(), req)
                .flatMap(record -> difyNoteService.contentAssistance(record.getRawNote(), req)
                        .map(providedAssistantDirection -> {
                            ProvideAssistantDirection result = new ProvideAssistantDirection();
                            result.setAvailableAssistantDirection(providedAssistantDirection.getRewriting());
                            result.setReason(providedAssistantDirection.getReason());
                            return NoteBaseResponse.success(result);
                        }))
                .switchIfEmpty(Mono.just(NoteBaseResponse.failure("找不到相应辅助记录")));
    }
}
