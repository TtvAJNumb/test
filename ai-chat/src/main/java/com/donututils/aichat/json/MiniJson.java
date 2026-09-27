package com.donututils.aichat.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small recursive-descent JSON parser/writer - just enough to round-trip the Anthropic Messages API's
 * request/response shapes (including tool-use content blocks) without pulling in a JSON library
 * dependency. Not hardened against malformed input beyond what a real API would ever send us. */
public final class MiniJson {

    private final String src;
    private int pos;

    private MiniJson(String src) {
        this.src = src;
    }

    public static Object parse(String text) {
        MiniJson parser = new MiniJson(text);
        parser.skipWhitespace();
        return parser.parseValue();
    }

    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        writeValue(value, out);
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(Object value, StringBuilder out) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String s) {
            out.append(JsonUtil.quoted(s));
        } else if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                out.append(JsonUtil.quoted(String.valueOf(entry.getKey()))).append(':');
                writeValue(entry.getValue(), out);
            }
            out.append('}');
        } else if (value instanceof List<?> list) {
            out.append('[');
            boolean first = true;
            for (Object item : list) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                writeValue(item, out);
            }
            out.append(']');
        } else if (value instanceof Boolean b) {
            out.append(b.toString());
        } else if (value instanceof Double d) {
            if (d == Math.floor(d) && !d.isInfinite() && Math.abs(d) < 1e15) {
                out.append((long) (double) d);
            } else {
                out.append(d);
            }
        } else {
            out.append(value);
        }
    }

    private Object parseValue() {
        skipWhitespace();
        char c = src.charAt(pos);
        return switch (c) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> parseString();
            case 't' -> {
                pos += 4;
                yield Boolean.TRUE;
            }
            case 'f' -> {
                pos += 5;
                yield Boolean.FALSE;
            }
            case 'n' -> {
                pos += 4;
                yield null;
            }
            default -> parseNumber();
        };
    }

    private Map<String, Object> parseObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        pos++;
        skipWhitespace();
        if (src.charAt(pos) == '}') {
            pos++;
            return map;
        }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            pos++;
            Object value = parseValue();
            map.put(key, value);
            skipWhitespace();
            char c = src.charAt(pos);
            pos++;
            if (c == '}') {
                break;
            }
        }
        return map;
    }

    private List<Object> parseArray() {
        List<Object> list = new ArrayList<>();
        pos++;
        skipWhitespace();
        if (src.charAt(pos) == ']') {
            pos++;
            return list;
        }
        while (true) {
            list.add(parseValue());
            skipWhitespace();
            char c = src.charAt(pos);
            pos++;
            if (c == ']') {
                break;
            }
        }
        return list;
    }

    private String parseString() {
        pos++;
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = src.charAt(pos);
            if (c == '"') {
                pos++;
                break;
            }
            if (c == '\\') {
                char next = src.charAt(pos + 1);
                switch (next) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> {
                        String hex = src.substring(pos + 2, pos + 6);
                        sb.append((char) Integer.parseInt(hex, 16));
                        pos += 4;
                    }
                    default -> sb.append(next);
                }
                pos += 2;
            } else {
                sb.append(c);
                pos++;
            }
        }
        return sb.toString();
    }

    private Double parseNumber() {
        int start = pos;
        while (pos < src.length() && "-+.eE0123456789".indexOf(src.charAt(pos)) >= 0) {
            pos++;
        }
        return Double.parseDouble(src.substring(start, pos));
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            pos++;
        }
    }
}
