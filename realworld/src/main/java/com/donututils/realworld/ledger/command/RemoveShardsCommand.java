package com.donututils.realworld.ledger.command;

import com.donututils.realworld.ledger.shards.ShardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** Top-level `/removeshards <player> <amount>` - StoreBridge runs this on a chargeback/refund to
 * claw back a delivered Shards package. Silently clamps to the player's current balance rather than
 * failing outright, since a chargeback must always succeed from the store's point of view even if the
 * player already spent some of what they were given. */
public final class RemoveShardsCommand implements CommandExecutor {

    private final ShardManager shardManager;

    public RemoveShardsCommand(ShardManager shardManager) {
        this.shardManager = shardManager;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ledger.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /removeshards <player> <amount>"));
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
        if (amount <= 0) {
            sender.sendMessage(color("&cAmount must be positive."));
            return true;
        }
        long current = shardManager.balance(target.getUniqueId());
        long toRemove = Math.min(current, amount);
        if (toRemove > 0) {
            shardManager.debit(target.getUniqueId(), toRemove);
        }
        sender.sendMessage(color("&aRemoved " + toRemove + " Shards from " + args[0] + "."));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
