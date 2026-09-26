package com.donututils.marketwatch.model;

public record AuctionSaleRecord(long listingId, String material, double price, String category, long soldAt) {

    public String toLine() {
        return listingId + "|" + material + "|" + price + "|" + safeCategory() + "|" + soldAt;
    }

    private String safeCategory() {
        String value = category == null || category.isBlank() ? "ALL" : category;
        return value.replace('|', '_');
    }

    public static AuctionSaleRecord fromLine(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 5) {
            return null;
        }
        try {
            return new AuctionSaleRecord(
                    Long.parseLong(parts[0]),
                    parts[1],
                    Double.parseDouble(parts[2]),
                    parts[3],
                    Long.parseLong(parts[4])
            );
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
