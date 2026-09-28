package com.donututils.realworld.municipal.claim;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;

/** Blocks breaking, placing, or interacting with blocks inside a chunk claimed by someone else. */
public final class ClaimProtectionListener implements Listener {

    private static final String DENY_MESSAGE = "&cThis land is claimed by someone else.";

    private final ClaimManager claimManager;

    public ClaimProtectionListener(ClaimManager claimManager) {
        this.claimManager = claimManager;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (claimManager.isProtected(event.getBlock().getLocation(), player)) {
            event.setCancelled(true);
            deny(player);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (claimManager.isProtected(event.getBlock().getLocation(), player)) {
            event.setCancelled(true);
            deny(player);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        Player player = event.getPlayer();
        if (claimManager.isProtected(event.getClickedBlock().getLocation(), player)) {
            event.setCancelled(true);
            deny(player);
        }
    }

    private void deny(Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', DENY_MESSAGE));
    }
}
