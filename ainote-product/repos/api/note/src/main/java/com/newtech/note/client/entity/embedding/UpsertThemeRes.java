package com.newtech.note.client.entity.embedding;

import java.util.List;

public record UpsertThemeRes(String user_id, String theme_id, List<String> related_note_ids) {
}
