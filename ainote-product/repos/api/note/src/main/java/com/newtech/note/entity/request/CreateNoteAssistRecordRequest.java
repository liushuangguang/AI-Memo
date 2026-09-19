package com.newtech.note.entity.request;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNoteAssistRecordRequest {
    @Schema(description = "笔记id, notes集合的ID")
    private String noteId;
}