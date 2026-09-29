package com.donututils.realworld.ledger.command;

import com.donututils.realworld.ledger.shards.ShardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** Top-level `/addshards <player> <amount>`, matching the exact command name StoreBridge's
 * config.yml runs to deliver a Shards store package (was `ultimatedonutsmp:addshards` against the
 * real UltimateDonutSmp - drop the `ultimatedonutsmp:` plugin-namespace prefix in that config now
 * that this plugin registers the bare command itself). */
public final class AddShardsCommand implements CommandExecutor {

    private final ShardManager shardManager;

    public AddShardsCommand(ShardManager shardManager) {
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
            sender.sendMessage(color("&cUsage: /addshards <player> <amount>"));
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
        shardManager.credit(target.getUniqueId(), amount);
        sender.sendMessage(color("&aGave " + amount + " Shards to " + args[0] + "."));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
