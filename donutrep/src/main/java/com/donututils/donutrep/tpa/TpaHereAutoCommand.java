package com.donututils.donutrep.tpa;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /tpahereauto - toggles auto-accepting every incoming /tpahere request. */
public final class TpaHereAutoCommand implements CommandExecutor {

    private final TpaManager tpaManager;

    public TpaHereAutoCommand(TpaManager tpaManager) {
        this.tpaManager = tpaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        boolean enabled = tpaManager.toggleAutoTpaHere(player.getUniqueId());
        player.sendMessage(color(enabled ? "&aAuto-accepting /tpahere requests." : "&7No longer auto-accepting /tpahere requests."));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
