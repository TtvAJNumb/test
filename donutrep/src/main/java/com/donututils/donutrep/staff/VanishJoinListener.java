package com.donututils.donutrep.staff;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class VanishJoinListener implements Listener {

    private final VanishManager vanishManager;

    public VanishJoinListener(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        vanishManager.syncOnJoin(event.getPlayer());
    }
}
