package com.donututils.donutrep.market.command;

import com.donututils.donutrep.economy.ShardManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/** /shards - matches real UDS exactly: no argument shows the sender's own Shards balance (there's no
 * way to look up another player's by name, unlike DonutREP's earlier version of this command). The
 * real admin-only branch, "/shards everywhere <status|debug> [player]", gates an unrelated "sell from
 * anywhere" eligibility system that doesn't exist in DonutREP yet - that subcommand is routed here
 * (so it doesn't silently fall through to the usage message) but intentionally not implemented, rather
 * than guessing at behavior with nothing to build it from. */
public final class ShardsCommand implements CommandExecutor {

    private final ShardManager shardManager;

    public ShardsCommand(ShardManager shardManager) {
        this.shardManager = shardManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("everywhere")) {
            return handleEverywhere(sender, args);
        }
        if (args.length > 0) {
            sender.sendMessage(color("&cUsage: /shards"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cUsage: /shards (run as a player to see your own balance)."));
            return true;
        }
        long balance = shardManager.balance(player.getUniqueId());
        player.sendMessage(color("&dYour Shards: &f" + balance));
        return true;
    }

    private boolean handleEverywhere(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ultimatedonutsmp.admin.shards")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return true;
        }
        if (args.length < 2 || !(args[1].equalsIgnoreCase("status") || args[1].equalsIgnoreCase("debug"))) {
            sender.sendMessage(color("&cUsage: /shards everywhere <status|debug> [player]"));
            return true;
        }
        sender.sendMessage(color("&7The sell-from-anywhere eligibility system isn't built in DonutREP yet."));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
