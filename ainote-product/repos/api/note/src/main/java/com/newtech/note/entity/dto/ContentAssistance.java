package com.newtech.note.entity.dto;

import lombok.Data;

import java.util.List;

@Data
public class ContentAssistance {
    private List<String> rewriting;
    private String reason;
}
