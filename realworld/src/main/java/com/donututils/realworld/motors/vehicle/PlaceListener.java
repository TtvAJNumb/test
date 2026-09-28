package com.donututils.realworld.motors.vehicle;

import com.donututils.realworld.motors.config.MotorsConfig;
import com.donututils.realworld.motors.config.VehicleDefinition;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.function.Supplier;

/** Right-clicking a block with a vehicle "spawner key" item consumes it and places the real
 * vehicle entity on top of that block, facing the way the player was facing. */
public final class PlaceListener implements Listener {

    private final VehicleManager vehicleManager;
    private final VehicleKeys keys;
    private final Supplier<MotorsConfig> configSupplier;

    public PlaceListener(VehicleManager vehicleManager, VehicleKeys keys, Supplier<MotorsConfig> configSupplier) {
        this.vehicleManager = vehicleManager;
        this.keys = keys;
        this.configSupplier = configSupplier;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        String vehicleId = readVehicleId(held);
        if (vehicleId == null) {
            return;
        }
        VehicleDefinition definition = configSupplier.get().vehicle(vehicleId);
        if (definition == null) {
            return;
        }

        event.setCancelled(true);

        Location base = clicked.getLocation();
        Location spawnAt = new Location(base.getWorld(), base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5,
                player.getLocation().getYaw(), 0f);

        vehicleManager.spawn(player, definition, spawnAt);
        held.setAmount(held.getAmount() - 1);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aPlaced your " + definition.displayName() + "&a."));
    }

    private String readVehicleId(ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(keys.vehicleId, PersistentDataType.STRING);
    }
}
