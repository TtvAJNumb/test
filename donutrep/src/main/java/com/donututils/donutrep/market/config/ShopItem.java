package com.donututils.donutrep.market.config;

import org.bukkit.Material;

import java.util.List;

/** One buyable line inside a {@link ShopMenu}, matching a real UDS shop.yml item entry verbatim
 * (display name, lore, slot). Exactly one of {@code crateId} or {@code spawnerMob} is set for the
 * special CRATE-KEYS and SHARD categories (the key is granted through the real Crates subsystem
 * instead of handed over as a plain item, and the spawner mob name drives a simplified, disclosed
 * placeholder since no full /spawner system exists yet) - everything else is a plain "give this
 * Material" purchase. */
public record ShopItem(
        String id,
        String displayName,
        List<String> lore,
        Material material,
        double price,
        Currency currency,
        int slot,
        int maxQuantity,
        String crateId,
        String spawnerMob
) {
}
