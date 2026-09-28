package com.donututils.realworld.arsenal.weapon;

import com.donututils.realworld.arsenal.config.WeaponDefinition;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class WeaponItemFactory {

    private final WeaponKeys keys;

    public WeaponItemFactory(WeaponKeys keys) {
        this.keys = keys;
    }

    public ItemStack createWeapon(WeaponDefinition definition) {
        ItemStack item = new ItemStack(definition.material());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(definition.displayName()));
            meta.setCustomModelData(definition.customModelData());
            meta.setLore(List.of(color("&7Ammo: &f" + definition.magazineSize() + "&7/" + definition.magazineSize())));
            meta.getPersistentDataContainer().set(keys.weaponId, PersistentDataType.STRING, definition.id());
            meta.getPersistentDataContainer().set(keys.ammoCount, PersistentDataType.INTEGER, definition.magazineSize());
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createAmmo(WeaponDefinition definition, int quantity) {
        ItemStack item = new ItemStack(definition.ammoMaterial(), Math.max(1, quantity));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(definition.ammoDisplayName()));
            meta.getPersistentDataContainer().set(keys.ammoForWeaponId, PersistentDataType.STRING, definition.id());
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Rewrites the ammo count in an item's lore and PDC after a shot or reload. */
    public void updateAmmoDisplay(ItemStack weaponItem, int currentAmmo, int magazineSize) {
        ItemMeta meta = weaponItem.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.setLore(List.of(color("&7Ammo: &f" + currentAmmo + "&7/" + magazineSize)));
        meta.getPersistentDataContainer().set(keys.ammoCount, PersistentDataType.INTEGER, currentAmmo);
        weaponItem.setItemMeta(meta);
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
