package com.newtech.note.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiResponseSanitizerTest {
    @Test
    void stripsLeadingThinkBlockBeforeJson() {
        assertThat(AiResponseSanitizer.stripMarkdownFence(
                "<think>internal reasoning</think>{\"title\":\"买菜\"}"))
                .isEqualTo("{\"title\":\"买菜\"}");
    }

    @Test
    void stripsMultipleLeadingThinkBlocksIgnoringCaseAndWhitespace() {
        assertThat(AiResponseSanitizer.stripMarkdownFence(
                " <THINK>one</think>\n<think>two</THINK>\n{\"ok\":true} "))
                .isEqualTo("{\"ok\":true}");
    }

    @Test
    void stripsThinkBlockThenMarkdownFence() {
        assertThat(AiResponseSanitizer.stripMarkdownFence(
                "<think>internal reasoning</think>\n```json\n{\"ok\":true}\n```"))
                .isEqualTo("{\"ok\":true}");
    }

    @Test
    void preservesUnclosedLeadingThinkBlock() {
        String response = "<think>internal reasoning\n{\"ok\":true}";

        assertThat(AiResponseSanitizer.stripMarkdownFence(response)).isEqualTo(response);
    }

    @Test
    void preservesResponseWithoutThinkBlock() {
        assertThat(AiResponseSanitizer.stripMarkdownFence("{\"ok\":true}"))
                .isEqualTo("{\"ok\":true}");
    }
}
