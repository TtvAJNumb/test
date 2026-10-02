package com.donututils.donutrep.staff;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /god [player] (alias godmode) - toggles damage immunity. */
public final class GodCommand implements CommandExecutor {

    private final GodManager godManager;

    public GodCommand(GodManager godManager) {
        this.godManager = godManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("staff.god.others")) {
                sender.sendMessage(color("&cYou do not have permission to toggle another player's god mode."));
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(color("&c" + args[0] + " isn't online."));
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(color("&cUsage: /god <player>"));
            return true;
        }

        boolean nowGod = godManager.toggle(target.getUniqueId());
        target.sendMessage(color(nowGod ? "&aGod mode enabled." : "&aGod mode disabled."));
        if (sender != target) {
            sender.sendMessage(color("&aSet " + target.getName() + "'s god mode to " + nowGod + "."));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
