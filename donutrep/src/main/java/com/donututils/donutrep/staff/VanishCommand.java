package com.donututils.donutrep.staff;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /vanish [player] - toggles invisibility to everyone without staff.vanish.see. */
public final class VanishCommand implements CommandExecutor {

    private final VanishManager vanishManager;

    public VanishCommand(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("staff.vanish.others")) {
                sender.sendMessage(color("&cYou do not have permission to vanish another player."));
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
            sender.sendMessage(color("&cUsage: /vanish <player>"));
            return true;
        }

        boolean nowVanished = vanishManager.toggle(target);
        target.sendMessage(color(nowVanished ? "&7You are now vanished." : "&aYou are now visible."));
        if (sender != target) {
            sender.sendMessage(color("&aSet " + target.getName() + "'s vanish state to " + nowVanished + "."));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
