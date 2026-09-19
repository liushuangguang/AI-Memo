package com.newtech.note.entity.dto.noteLogs;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.newtech.note.common.enumeration.NoteOperationType;
import com.newtech.note.entity.dto.noteModules.NoteModule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

import java.time.LocalDateTime;

@Setter
@Getter
@BsonDiscriminator(value = "NOTE_OPERATION_LOG.REPLACE_TEXT_MODULE")
@Schema(description = "取代文本模块日志")
public class NoteReplaceTextModuleLog implements NoteOperationLog {
    @Schema(description = "取代文本模块操作类型")
    private NoteOperationType type = NoteOperationType.REPLACE_TEXT_MODULE;

    @Schema(description = "发生时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime occurredAt;

    @Schema(description = "原文本")
    private NoteModule oldModule;

    @Schema(description = "新文本")
    private NoteModule newModule;
}
