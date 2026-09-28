package com.donututils.realworld.municipal.command;

import com.donututils.realworld.municipal.permit.PermitManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Set;

public final class PermitCommand implements CommandExecutor {

    private final PermitManager permitManager;

    public PermitCommand(PermitManager permitManager) {
        this.permitManager = permitManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&cUsage: /permit [buy <business|building|weapon>|check [player]]"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "buy" -> buy(sender, args);
            case "check" -> check(sender, args);
            default -> sender.sendMessage(color("&cUsage: /permit [buy <business|building|weapon>|check [player]]"));
        }
        return true;
    }

    private void buy(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can buy permits."));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /permit buy <business|building|weapon>"));
            return;
        }
        PermitManager.PurchaseResult result = permitManager.buy(player, args[1]);
        player.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
    }

    @SuppressWarnings("deprecation")
    private void check(CommandSender sender, String[] args) {
        OfflinePlayer target;
        if (args.length >= 2) {
            target = Bukkit.getOfflinePlayer(args[1]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(color("&cUsage: /permit check <player>"));
            return;
        }
        Set<String> permits = permitManager.permitsOf(target.getUniqueId());
        sender.sendMessage(color("&6&l" + target.getName() + "'s Permits"));
        if (permits.isEmpty()) {
            sender.sendMessage(color("&7None."));
        } else {
            for (String type : permits) {
                sender.sendMessage(color("&a- " + type));
            }
        }
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
