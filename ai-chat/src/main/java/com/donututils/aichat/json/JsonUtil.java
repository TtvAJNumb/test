package com.donututils.aichat.json;

/** Minimal, dependency-free JSON helpers - escapes strings for building requests, and pulls a single
 * string field out of a response. Not a general-purpose parser, just enough for the flat shapes the
 * Anthropic Messages API sends/returns, matching the hand-rolled-JSON approach used elsewhere in this repo. */
public final class JsonUtil {

    private JsonUtil() {
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    public static String quoted(String value) {
        return "\"" + escape(value) + "\"";
    }

    /** Finds the first occurrence of "key" in the document and returns its string value, unescaped. */
    public static String extractString(String json, String key) {
        if (json == null) {
            return null;
        }
        String needle = "\"" + key + "\"";
        int idx = json.indexOf(needle);
        if (idx < 0) {
            return null;
        }
        idx += needle.length();
        while (idx < json.length() && (json.charAt(idx) == ':' || Character.isWhitespace(json.charAt(idx)))) {
            idx++;
        }
        if (idx >= json.length() || json.charAt(idx) != '"') {
            return null;
        }
        idx++;
        StringBuilder out = new StringBuilder();
        while (idx < json.length()) {
            char c = json.charAt(idx);
            if (c == '\\' && idx + 1 < json.length()) {
                char next = json.charAt(idx + 1);
                switch (next) {
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case '/' -> out.append('/');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'u' -> {
                        if (idx + 5 < json.length()) {
                            out.append((char) Integer.parseInt(json.substring(idx + 2, idx + 6), 16));
                            idx += 4;
                        }
                    }
                    default -> out.append(next);
                }
                idx += 2;
            } else if (c == '"') {
                break;
            } else {
                out.append(c);
                idx++;
            }
        }
        return out.toString();
    }
}
