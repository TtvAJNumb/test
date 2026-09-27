package com.donututils.stockmarket.model;

public record PriceTick(long timestampMillis, double open, double high, double low, double close, long volume) {

    public String toLine() {
        return timestampMillis + "|" + open + "|" + high + "|" + low + "|" + close + "|" + volume;
    }

    public static PriceTick fromLine(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 6) {
            return null;
        }
        try {
            return new PriceTick(
                    Long.parseLong(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]),
                    Double.parseDouble(parts[4]),
                    Long.parseLong(parts[5])
            );
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
