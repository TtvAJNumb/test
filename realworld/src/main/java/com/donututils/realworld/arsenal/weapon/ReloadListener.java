package com.donututils.realworld.arsenal.weapon;

import com.donututils.realworld.arsenal.config.WeaponDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

/** Repurposes the swap-hands key (F by default) as the reload key while holding a weapon - the only
 * otherwise-rarely-used vanilla keybind with its own event, which is why most gun plugins bind reload
 * to it instead of needing a custom keybind system. */
public final class ReloadListener implements Listener {

    private final WeaponManager weaponManager;

    public ReloadListener(WeaponManager weaponManager) {
        this.weaponManager = weaponManager;
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        WeaponDefinition definition = weaponManager.identify(held);
        if (definition == null) {
            return;
        }
        event.setCancelled(true);
        weaponManager.reload(player, held, definition);
    }
}
