package com.donututils.donutrep.staff;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public final class GodListener implements Listener {

    private final GodManager godManager;

    public GodListener(GodManager godManager) {
        this.godManager = godManager;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && godManager.isGod(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
