package com.donututils.donutrep.market.command;

import com.donututils.donutrep.DonutREPPlugin;
import com.donututils.donutrep.economy.ShardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Locale;

public final class ShopAdminCommand implements CommandExecutor {

    private final DonutREPPlugin plugin;
    private final ShardManager shardManager;

    public ShopAdminCommand(DonutREPPlugin plugin, ShardManager shardManager) {
        this.plugin = plugin;
        this.shardManager = shardManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("market.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(color("&cUsage: /shopadmin <reload|addshards <player> <amount>|setmultiplier <category> <value>>"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "addshards" -> addShards(sender, args);
            case "setmultiplier" -> setMultiplier(sender, args);
            default -> sender.sendMessage(color("&cUnknown /shopadmin subcommand."));
        }
        return true;
    }

    private void reload(CommandSender sender) {
        plugin.reloadMarket();
        sender.sendMessage(color("&aMarket config reloaded."));
    }

    @SuppressWarnings("deprecation")
    private void addShards(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(color("&cUsage: /shopadmin addshards <player> <amount>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        long amount;
        try {
            amount = Long.parseLong(args[2]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(color("&cInvalid amount."));
            return;
        }
        shardManager.credit(target.getUniqueId(), amount);
        sender.sendMessage(color("&aGave " + amount + " Shards to " + args[1] + "."));
    }

    private void setMultiplier(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(color("&cUsage: /shopadmin setmultiplier <category> <value>"));
            return;
        }
        double value;
        try {
            value = Double.parseDouble(args[2]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(color("&cInvalid value."));
            return;
        }
        plugin.getConfig().set("market.category-multipliers." + args[1].toLowerCase(Locale.ROOT), value);
        plugin.saveConfig();
        plugin.reloadMarket();
        sender.sendMessage(color("&aSet the '" + args[1] + "' category multiplier to " + value + "."));
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
