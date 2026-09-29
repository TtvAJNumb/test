package com.donututils.donutrep.orders;

import org.bukkit.Material;

import java.util.UUID;

public record BuyOrder(
        int id,
        UUID buyerId,
        String buyerName,
        Material material,
        int amount,
        double pricePerUnit,
        UUID fulfillerId,
        String fulfillerName,
        boolean collected
) {
    public boolean isFulfilled() {
        return fulfillerId != null;
    }

    public double totalPrice() {
        return pricePerUnit * amount;
    }
}
