package com.donututils.marketwatch.model;

public record EconomySnapshot(long timestampMillis, double totalMoney, int playerCount) {

    public String toLine() {
        return timestampMillis + "|" + totalMoney + "|" + playerCount;
    }

    public static EconomySnapshot fromLine(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 3) {
            return null;
        }
        try {
            return new EconomySnapshot(Long.parseLong(parts[0]), Double.parseDouble(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
