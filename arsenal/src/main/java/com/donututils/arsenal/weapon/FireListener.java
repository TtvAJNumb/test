package com.donututils.arsenal.weapon;

import com.donututils.arsenal.config.WeaponDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public final class FireListener implements Listener {

    private final WeaponManager weaponManager;

    public FireListener(WeaponManager weaponManager) {
        this.weaponManager = weaponManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        WeaponDefinition definition = weaponManager.identify(held);
        if (definition == null) {
            return;
        }
        event.setCancelled(true);
        weaponManager.fire(player, held, definition);
    }
}
