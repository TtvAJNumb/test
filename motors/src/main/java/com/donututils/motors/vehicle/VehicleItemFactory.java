package com.donututils.motors.vehicle;

import com.donututils.motors.config.VehicleDefinition;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class VehicleItemFactory {

    private final VehicleKeys keys;

    public VehicleItemFactory(VehicleKeys keys) {
        this.keys = keys;
    }

    /** A one-use "spawner key" item: right-clicking a block with it consumes the item and places the
     * real vehicle entity. Wears the vehicle's own custom-model-data so it previews correctly in
     * the resource pack before it's ever placed. */
    public ItemStack createSpawnerKey(VehicleDefinition definition) {
        ItemStack item = new ItemStack(definition.keyMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(definition.keyDisplayName()));
            meta.setCustomModelData(definition.keyCustomModelData());
            meta.setLore(List.of(color("&7Right-click a block to place your " + definition.displayName())));
            meta.getPersistentDataContainer().set(keys.vehicleId, PersistentDataType.STRING, definition.id());
            item.setItemMeta(meta);
        }
        return item;
    }

    /** The decorative "body" item worn as a helmet by the invisible ArmorStand riding alongside the
     * real vanilla vehicle - this is what the resource pack re-textures into an actual vehicle model. */
    public ItemStack createBodyItem(VehicleDefinition definition) {
        ItemStack item = new ItemStack(definition.bodyHelmetMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(definition.bodyHelmetCustomModelData());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
