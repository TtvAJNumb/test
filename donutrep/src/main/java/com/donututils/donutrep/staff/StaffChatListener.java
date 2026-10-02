package com.donututils.donutrep.staff;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/** Reroutes a chat line into staff chat instead of public chat for anyone with "always staffchat"
 * toggled on via /staffchat. */
public final class StaffChatListener implements Listener {

    private final StaffChatManager staffChatManager;
    private final StaffChatCommand staffChatCommand;

    public StaffChatListener(StaffChatManager staffChatManager, StaffChatCommand staffChatCommand) {
        this.staffChatManager = staffChatManager;
        this.staffChatCommand = staffChatCommand;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!staffChatManager.isAlwaysOn(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        staffChatCommand.broadcast(player.getName(), event.getMessage());
    }
}
