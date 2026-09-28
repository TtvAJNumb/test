package com.donututils.realworld.dynamicshop.model;

import java.util.UUID;

public record TransactionRecord(UUID playerId, String material, String side, long quantity,
                                 double pricePerUnit, double totalAmount, String currency, long timestamp) {

    public String toLine() {
        return playerId + "|" + material + "|" + side + "|" + quantity + "|" + pricePerUnit + "|"
                + totalAmount + "|" + currency + "|" + timestamp;
    }

    public static TransactionRecord fromLine(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 8) {
            return null;
        }
        try {
            return new TransactionRecord(
                    UUID.fromString(parts[0]),
                    parts[1],
                    parts[2],
                    Long.parseLong(parts[3]),
                    Double.parseDouble(parts[4]),
                    Double.parseDouble(parts[5]),
                    parts[6],
                    Long.parseLong(parts[7])
            );
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
