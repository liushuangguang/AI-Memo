package com.newtech.note.entity.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "note_theme_merge_operations")
public class NoteThemeMergeOperation {
    @Id
    private String id;
    private String deviceId;
    private String themeId;
    private String requestKey;
    private String status;
    private String mergedNoteId;
    private String mergedTitle;
    private String mergedContent;
    private List<String> imageUrls;
    private List<NoteThemeSourceRef> sources;
    private LocalDateTime mergedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
