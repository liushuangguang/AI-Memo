package com.newtech.note.entity.request.voice;

import java.util.List;

public record VoiceDiscussionRequest(String noteId,
                                     String message,
                                     List<VoiceDiscussionTurn> history) {
}
