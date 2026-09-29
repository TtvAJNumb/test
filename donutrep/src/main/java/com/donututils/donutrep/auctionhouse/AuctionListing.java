package com.donututils.donutrep.auctionhouse;

import org.bukkit.Material;

public record AuctionListing(
        int id,
        java.util.UUID sellerId,
        String sellerName,
        Material material,
        int amount,
        String displayName,
        double price,
        long listedAtMillis
) {
}
