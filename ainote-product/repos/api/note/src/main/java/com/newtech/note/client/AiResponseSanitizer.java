package com.newtech.note.client;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AiResponseSanitizer {
    private static final Pattern LEADING_THINK_OPEN =
            Pattern.compile("^<think\\s*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern THINK_CLOSE =
            Pattern.compile("</think\\s*>", Pattern.CASE_INSENSITIVE);

    private AiResponseSanitizer() {
    }

    static String stripMarkdownFence(String value) {
        String normalized = StringUtils.defaultString(value).trim();
        String withoutLeadingThink = stripLeadingThinkBlocks(normalized);
        if (withoutLeadingThink == null) {
            return normalized;
        }
        normalized = withoutLeadingThink;
        int openingFence = normalized.indexOf("```");
        if (openingFence < 0) {
            return normalized;
        }
        int firstLineEnd = normalized.indexOf('\n', openingFence + 3);
        int closingFence = firstLineEnd < 0 ? -1 : normalized.indexOf("```", firstLineEnd + 1);
        if (firstLineEnd < 0 || closingFence <= firstLineEnd) {
            return normalized;
        }
        return normalized.substring(firstLineEnd + 1, closingFence).trim();
    }

    private static String stripLeadingThinkBlocks(String normalized) {
        String remaining = normalized;
        boolean stripped = false;
        while (true) {
            Matcher opening = LEADING_THINK_OPEN.matcher(remaining);
            if (!opening.find()) {
                return stripped ? remaining : normalized;
            }
            Matcher closing = THINK_CLOSE.matcher(remaining);
            if (!closing.find(opening.end())) {
                return null;
            }
            remaining = remaining.substring(closing.end()).trim();
            stripped = true;
        }
    }
}
