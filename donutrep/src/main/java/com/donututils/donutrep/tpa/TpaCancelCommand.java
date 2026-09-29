package com.donututils.donutrep.tpa;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /tpacancel - cancels every teleport request you've sent that's still pending. */
public final class TpaCancelCommand implements CommandExecutor {

    private final TpaManager tpaManager;

    public TpaCancelCommand(TpaManager tpaManager) {
        this.tpaManager = tpaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        int cancelled = tpaManager.cancel(player.getUniqueId());
        player.sendMessage(color(cancelled > 0 ? "&7Cancelled " + cancelled + " pending request(s)." : "&cYou have no pending requests to cancel."));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
