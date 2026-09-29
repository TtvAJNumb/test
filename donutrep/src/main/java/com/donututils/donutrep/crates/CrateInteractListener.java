package com.donututils.donutrep.crates;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class CrateInteractListener implements Listener {

    private final CrateManager crateManager;

    public CrateInteractListener(CrateManager crateManager) {
        this.crateManager = crateManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        String crateId = crateManager.boundCrateId(block);
        if (crateId == null) {
            return;
        }
        Player player = event.getPlayer();
        event.setCancelled(true);
        if (!crateManager.tryOpen(player, crateId)) {
            player.sendMessage("§cYou need a " + crateId + " crate key to open this.");
        }
    }
}
