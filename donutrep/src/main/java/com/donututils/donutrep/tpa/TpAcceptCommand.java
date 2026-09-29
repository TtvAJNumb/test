package com.donututils.donutrep.tpa;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /tpaccept [player] - accepts a pending teleport request. */
public final class TpAcceptCommand implements CommandExecutor {

    private final TpaManager tpaManager;

    public TpAcceptCommand(TpaManager tpaManager) {
        this.tpaManager = tpaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        String requesterName = args.length >= 1 ? args[0] : null;
        String error = tpaManager.accept(player, requesterName, Bukkit::getPlayer);
        if (error != null) {
            player.sendMessage(color("&c" + error));
        } else {
            player.sendMessage(color("&aRequest accepted."));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
