package com.newtech.note.entity.request;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "验证笔记请求")
public class ValidateNoteRequest {
    @Schema(description = "笔记id")
    private String id;

}
