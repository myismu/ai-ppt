package com.exambaby.ppt.service.support;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * JSONL 行过滤工具
 */
public final class JsonlSlideSanitizer {

    private static final Set<String> VALID_TYPES = new HashSet<>(Arrays.asList(
            "cover", "contents", "transition", "content", "end"
    ));

    private JsonlSlideSanitizer() {
    }

    public static String sanitize(String content) {
        if (content == null || content.isBlank()) {
            return "{\"type\": \"end\"}";
        }

        StringBuilder builder = new StringBuilder();
        boolean hasEnd = false;
        String normalized = content.replace("```jsonl", "")
                .replace("```json", "")
                .replace("```", "");

        for (String line : normalized.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || !trimmed.startsWith("{")) {
                continue;
            }
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (!containsValidType(lower)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(System.lineSeparator());
            }
            builder.append(trimmed);
            if (lower.contains("\"type\": \"end\"") || lower.contains("\"type\":\"end\"")) {
                hasEnd = true;
            }
        }

        if (builder.length() == 0) {
            builder.append("{\"type\": \"end\"}");
            return builder.toString();
        }

        if (!hasEnd) {
            builder.append(System.lineSeparator()).append("{\"type\": \"end\"}");
        }
        return builder.toString();
    }

    private static boolean containsValidType(String line) {
        for (String type : VALID_TYPES) {
            if (line.contains("\"type\": \"" + type + "\"") || line.contains("\"type\":\"" + type + "\"")) {
                return true;
            }
        }
        return false;
    }
}
