package com.donututils.realworld.crates;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class CrateItemFactory {

    private final CrateKeys keys;

    public CrateItemFactory(CrateKeys keys) {
        this.keys = keys;
    }

    public ItemStack createKey(CrateDefinition definition, int amount) {
        ItemStack item = new ItemStack(definition.keyMaterial(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&e&l" + definition.displayName() + " Key"));
            meta.setCustomModelData(definition.keyCustomModelData());
            meta.setLore(List.of(color("&7Right-click a bound " + definition.displayName() + " crate to open it.")));
            meta.getPersistentDataContainer().set(keys.crateId, PersistentDataType.STRING, definition.id());
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isKeyFor(ItemStack item, String crateId) {
        if (item == null || item.getItemMeta() == null) {
            return false;
        }
        String id = item.getItemMeta().getPersistentDataContainer().get(keys.crateId, PersistentDataType.STRING);
        return id != null && id.equalsIgnoreCase(crateId);
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
