package com.actacofrade.backend.util;

import java.util.regex.Pattern;

public final class SanitizationUtils {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s{2,}");

    private SanitizationUtils() {
    }

    public static String sanitize(String input) {
        String result = input;
        if (result != null) {
            result = result.trim();
            result = HTML_TAG_PATTERN.matcher(result).replaceAll("");
            result = MULTI_SPACE_PATTERN.matcher(result).replaceAll(" ");
            result = result.trim();
        }
        return result;
    }

    public static String sanitizeNullable(String input) {
        String result = null;
        if (input != null && !input.isBlank()) {
            result = sanitize(input);
        }
        return result;
    }
}
