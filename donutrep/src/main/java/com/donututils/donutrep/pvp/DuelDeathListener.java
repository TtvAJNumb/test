package com.donututils.donutrep.pvp;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Dying (or disconnecting) while in an active duel ends it in the opponent's favor. */
public final class DuelDeathListener implements Listener {

    private final DuelManager duelManager;

    public DuelDeathListener(DuelManager duelManager) {
        this.duelManager = duelManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        duelManager.end(player.getUniqueId(), "died");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        duelManager.end(event.getPlayer().getUniqueId(), "disconnected");
    }
}
