package com.newtech.note.entity.dto.noteLogs;

import com.newtech.note.common.enumeration.NoteOperationType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

import java.time.LocalDateTime;

@BsonDiscriminator
@Schema(description = "Note操作日志接口")
public interface NoteOperationLog {
    NoteOperationType getType();

    LocalDateTime getOccurredAt();
}
