package com.donututils.punishhistory.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** A freeform staff note. UltimateDonutSmp has no notes concept of its own - this plugin owns it. */
public record Note(long timestamp, String authorName, String text) {

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", timestamp);
        map.put("authorName", authorName);
        map.put("text", text);
        return map;
    }

    public static Note fromMap(Map<?, ?> map) {
        Object ts = map.get("timestamp");
        long timestamp = ts instanceof Number n ? n.longValue() : 0L;
        return new Note(timestamp, String.valueOf(map.get("authorName")), String.valueOf(map.get("text")));
    }
}
