package com.newtech.note.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteThemeCandidate {
    private String id;
    private String title;
    private String content;
    private String imageUrl;
    private double score;
    private String reason;
}
