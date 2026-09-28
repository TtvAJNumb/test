package com.donututils.realworld.market.command;

import com.donututils.realworld.ledger.shards.ShardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ShardsCommand implements CommandExecutor {

    private final ShardManager shardManager;

    public ShardsCommand(ShardManager shardManager) {
        this.shardManager = shardManager;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        OfflinePlayer target;
        if (args.length >= 1) {
            target = Bukkit.getOfflinePlayer(args[0]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(color("&cUsage: /shards <player>"));
            return true;
        }
        long balance = shardManager.balance(target.getUniqueId());
        sender.sendMessage(color("&d" + (args.length >= 1 ? target.getName() + "'s" : "Your") + " Shards: &f" + balance));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
