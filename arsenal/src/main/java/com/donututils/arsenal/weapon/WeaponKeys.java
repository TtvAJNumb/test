package com.donututils.arsenal.weapon;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/** NamespacedKeys used to tag weapon/ammo items via their PersistentDataContainer, so identity and
 * ammo count travel with the actual ItemStack (survives drops, pickups, inventory moves) instead of
 * needing a separate lookup table. */
public final class WeaponKeys {

    public final NamespacedKey weaponId;
    public final NamespacedKey ammoCount;
    public final NamespacedKey ammoForWeaponId;

    public WeaponKeys(Plugin plugin) {
        this.weaponId = new NamespacedKey(plugin, "weapon_id");
        this.ammoCount = new NamespacedKey(plugin, "ammo_count");
        this.ammoForWeaponId = new NamespacedKey(plugin, "ammo_for_weapon_id");
    }
}
