package com.donututils.donutrep.tpa;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /tpauto - toggles auto-accepting every incoming /tpa request. */
public final class TpAutoCommand implements CommandExecutor {

    private final TpaManager tpaManager;

    public TpAutoCommand(TpaManager tpaManager) {
        this.tpaManager = tpaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        boolean enabled = tpaManager.toggleAutoTpa(player.getUniqueId());
        player.sendMessage(color(enabled ? "&aAuto-accepting /tpa requests." : "&7No longer auto-accepting /tpa requests."));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
