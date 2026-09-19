package com.newtech.note.entity.request.voice;

public record SaveVoiceDiscussionSummaryRequest(String noteId,
                                                String summary,
                                                String saveRequestId) {
}
