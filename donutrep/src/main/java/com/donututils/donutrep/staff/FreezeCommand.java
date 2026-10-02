package com.donututils.donutrep.staff;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /freeze <player> - standalone toggle, same effect as the /sus panel's freeze button. */
public final class FreezeCommand implements CommandExecutor {

    private final FreezeManager freezeManager;

    public FreezeCommand(FreezeManager freezeManager) {
        this.freezeManager = freezeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(color("&cUsage: /freeze <player>"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(color("&c" + args[0] + " isn't online."));
            return true;
        }
        if (target.hasPermission("staff.freezeexempt")) {
            sender.sendMessage(color("&c" + target.getName() + " is exempt from freezing."));
            return true;
        }
        boolean nowFrozen = freezeManager.toggle(target.getUniqueId());
        sender.sendMessage(color((nowFrozen ? "&aFroze " : "&aUnfroze ") + target.getName() + "&a."));
        target.sendMessage(color(nowFrozen ? "&cYou have been frozen by staff." : "&aYou've been unfrozen."));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
