package com.donututils.ecowatch.model;

import java.util.UUID;

/** A completed (SOLD) UltimateDonutSmp auction house listing, flattened out of reflection. */
public record AuctionSaleSnapshot(
        long listingId,
        UUID sellerUuid,
        String sellerName,
        UUID buyerUuid,
        double price,
        long soldAt,
        String category
) {
}
