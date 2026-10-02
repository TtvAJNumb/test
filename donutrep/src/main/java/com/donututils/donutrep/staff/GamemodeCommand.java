package com.donututils.donutrep.staff;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/** /gamemode <survival|creative|adventure|spectator> [player] (aliases gmc/gms/gma/gmsp are registered
 * to the same executor, each pre-selecting a mode). */
public final class GamemodeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        GameMode mode = modeFromLabel(label);
        String[] targetArgs = args;
        if (mode == null) {
            if (args.length < 1) {
                sender.sendMessage(color("&cUsage: /gamemode <survival|creative|adventure|spectator> [player]"));
                return true;
            }
            mode = parseMode(args[0]);
            if (mode == null) {
                sender.sendMessage(color("&cUnknown game mode '" + args[0] + "'."));
                return true;
            }
            targetArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
        }

        Player target;
        if (targetArgs.length >= 1) {
            if (!sender.hasPermission("staff.gamemode.others")) {
                sender.sendMessage(color("&cYou do not have permission to set another player's game mode."));
                return true;
            }
            target = Bukkit.getPlayer(targetArgs[0]);
            if (target == null) {
                sender.sendMessage(color("&c" + targetArgs[0] + " isn't online."));
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(color("&cUsage: /gamemode <mode> <player>"));
            return true;
        }

        target.setGameMode(mode);
        target.sendMessage(color("&aGame mode set to " + mode.name().toLowerCase(Locale.ROOT) + "."));
        if (sender != target) {
            sender.sendMessage(color("&aSet " + target.getName() + "'s game mode to " + mode.name().toLowerCase(Locale.ROOT) + "."));
        }
        return true;
    }

    private static GameMode modeFromLabel(String label) {
        return switch (label.toLowerCase(Locale.ROOT)) {
            case "gmc" -> GameMode.CREATIVE;
            case "gms" -> GameMode.SURVIVAL;
            case "gma" -> GameMode.ADVENTURE;
            case "gmsp" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    private static GameMode parseMode(String arg) {
        String normalized = arg.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "survival", "s", "0" -> GameMode.SURVIVAL;
            case "creative", "c", "1" -> GameMode.CREATIVE;
            case "adventure", "a", "2" -> GameMode.ADVENTURE;
            case "spectator", "sp", "3" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
