package com.newtech.note.entity.request;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class MergeNoteThemeRequest {
    private List<String> sourceNoteIds;
    private boolean selectionConfirmed;
    private String idempotencyKey;
}
