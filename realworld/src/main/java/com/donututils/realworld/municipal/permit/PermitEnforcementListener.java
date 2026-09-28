package com.donututils.realworld.municipal.permit;

import com.donututils.realworld.municipal.config.MunicipalConfig;
import com.donututils.realworld.municipal.config.PermitDefinition;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

/** Enforces building and weapon permits. A player with {@code municipal.bypass} (ops by default)
 * skips both checks - staff shouldn't need a permit to do their job. */
public final class PermitEnforcementListener implements Listener {

    private final Supplier<MunicipalConfig> configSupplier;

    public PermitEnforcementListener(Supplier<MunicipalConfig> configSupplier) {
        this.configSupplier = configSupplier;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        MunicipalConfig config = configSupplier.get();
        Player player = event.getPlayer();
        if (player.hasPermission("municipal.bypass")) {
            return;
        }
        String worldName = event.getBlock().getWorld() != null ? event.getBlock().getWorld().getName() : null;
        if (worldName == null || !config.isCityLimitWorld(worldName)) {
            return;
        }
        PermitDefinition building = config.permit("building");
        if (building == null || player.hasPermission(building.permission())) {
            return;
        }
        event.setCancelled(true);
        player.sendMessage(legacy("&cYou need a building permit to place blocks here - see /permit buy building."));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        MunicipalConfig config = configSupplier.get();
        Player player = event.getPlayer();
        if (player.hasPermission("municipal.bypass")) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() == null) {
            return;
        }
        if (!config.isControlledWeapon(held.getType().name())) {
            return;
        }
        PermitDefinition weapon = config.permit("weapon");
        if (weapon == null || player.hasPermission(weapon.permission())) {
            return;
        }
        event.setCancelled(true);
        player.sendMessage(legacy("&cYou need a weapon permit to use that - see /permit buy weapon."));
    }

    private static String legacy(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
