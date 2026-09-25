package com.donututils.crateodds.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** One recorded crate opening, persisted to disk so it survives restarts. */
public record HistoryEntry(
        long timestamp,
        String crateId,
        String rewardId,
        String rewardDisplayName,
        String grantSummary,
        double percentAtTime
) {

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", timestamp);
        map.put("crateId", crateId);
        map.put("rewardId", rewardId);
        map.put("rewardDisplayName", rewardDisplayName);
        map.put("grantSummary", grantSummary);
        map.put("percentAtTime", percentAtTime);
        return map;
    }

    public static HistoryEntry fromMap(Map<?, ?> map) {
        return new HistoryEntry(
                asLong(map.get("timestamp")),
                String.valueOf(map.get("crateId")),
                String.valueOf(map.get("rewardId")),
                String.valueOf(map.get("rewardDisplayName")),
                String.valueOf(map.get("grantSummary")),
                asDouble(map.get("percentAtTime"))
        );
    }

    private static long asLong(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }

    private static double asDouble(Object o) {
        return o instanceof Number n ? n.doubleValue() : 0.0;
    }
}
