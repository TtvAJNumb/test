package com.donututils.realworld.crates;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/** NamespacedKeys tagging a crate key ItemStack's PersistentDataContainer, so identity travels with
 * the item (survives drops/pickups/inventory moves) instead of needing a separate lookup table - same
 * pattern as Arsenal's WeaponKeys and Motors' VehicleKeys. */
public final class CrateKeys {

    public final NamespacedKey crateId;

    public CrateKeys(Plugin plugin) {
        this.crateId = new NamespacedKey(plugin, "crate_id");
    }
}
