package com.donututils.donutrep.warps;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/** Walking into a portal region teleports the player to its linked warp. */
public final class PortalMoveListener implements Listener {

    private final PortalManager portalManager;
    private final WarpManager warpManager;

    public PortalMoveListener(PortalManager portalManager, WarpManager warpManager) {
        this.portalManager = portalManager;
        this.warpManager = warpManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!portalManager.canTeleport(player.getUniqueId())) {
            return;
        }
        PortalManager.Portal portal = portalManager.find(event.getTo());
        if (portal == null) {
            return;
        }
        Location destination = warpManager.warp(portal.destinationWarp());
        if (destination == null) {
            return;
        }
        portalManager.markTeleported(player.getUniqueId());
        player.teleport(destination);
    }
}
