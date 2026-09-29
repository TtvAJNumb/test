package com.donututils.donutrep.economy.command;

import com.donututils.donutrep.economy.ShardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /shardpay <player> <amount> - player-to-player Shards transfer, the Shards equivalent of /pay. */
public final class ShardPayCommand implements CommandExecutor {

    private final ShardManager shardManager;

    public ShardPayCommand(ShardManager shardManager) {
        this.shardManager = shardManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /shardpay <player> <amount>"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(color("&c" + args[0] + " isn't online."));
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(color("&cYou can't pay yourself."));
            return true;
        }
        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(color("&cInvalid amount."));
            return true;
        }
        if (amount <= 0) {
            player.sendMessage(color("&cAmount must be positive."));
            return true;
        }
        if (!shardManager.debit(player.getUniqueId(), amount)) {
            player.sendMessage(color("&cYou don't have " + amount + " Shards."));
            return true;
        }
        shardManager.credit(target.getUniqueId(), amount);
        player.sendMessage(color("&aSent " + amount + " Shards to " + target.getName() + "."));
        target.sendMessage(color("&a" + player.getName() + " sent you " + amount + " Shards."));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
