package com.donututils.donutrep.economy.command;

import com.donututils.donutrep.economy.ShardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** /setshards <player> <amount> - admin command to set a player's Shards balance exactly. */
public final class SetShardsCommand implements CommandExecutor {

    private final ShardManager shardManager;

    public SetShardsCommand(ShardManager shardManager) {
        this.shardManager = shardManager;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("economy.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /setshards <player> <amount>"));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(color("&cInvalid amount."));
            return true;
        }
        if (amount < 0) {
            sender.sendMessage(color("&cAmount cannot be negative."));
            return true;
        }
        long current = shardManager.balance(target.getUniqueId());
        if (amount > current) {
            shardManager.credit(target.getUniqueId(), amount - current);
        } else if (amount < current) {
            shardManager.debit(target.getUniqueId(), current - amount);
        }
        sender.sendMessage(color("&aSet " + args[0] + "'s Shards to " + amount + "."));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
