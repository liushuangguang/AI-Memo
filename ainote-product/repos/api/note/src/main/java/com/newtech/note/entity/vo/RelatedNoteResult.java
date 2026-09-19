package com.newtech.note.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RelatedNoteResult {
    private String id;
    private String title;
    private String content;
    private String reason;
    private double score;
}
