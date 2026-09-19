package com.newtech.note.entity.request.voice;

import java.util.List;

public record VoiceDiscussionSummaryRequest(String noteId,
                                            List<VoiceDiscussionTurn> history) {
}
