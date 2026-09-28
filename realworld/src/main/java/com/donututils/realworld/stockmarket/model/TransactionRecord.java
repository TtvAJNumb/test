package com.donututils.realworld.stockmarket.model;

import java.util.UUID;

public record TransactionRecord(
        long timestampMillis,
        UUID playerUuid,
        String playerName,
        String symbol,
        TransactionSide side,
        long shares,
        double pricePerShare,
        double fee
) {
    public double grossTotal() {
        return shares * pricePerShare;
    }

    public String toLine() {
        return timestampMillis + "|" + playerUuid + "|" + escape(playerName) + "|" + symbol + "|" + side
                + "|" + shares + "|" + pricePerShare + "|" + fee;
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace('|', '_');
    }

    public static TransactionRecord fromLine(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 8) {
            return null;
        }
        try {
            return new TransactionRecord(
                    Long.parseLong(parts[0]),
                    UUID.fromString(parts[1]),
                    parts[2],
                    parts[3],
                    TransactionSide.valueOf(parts[4]),
                    Long.parseLong(parts[5]),
                    Double.parseDouble(parts[6]),
                    Double.parseDouble(parts[7])
            );
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
