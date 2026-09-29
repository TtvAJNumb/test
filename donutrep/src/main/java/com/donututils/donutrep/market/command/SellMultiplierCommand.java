package com.donututils.donutrep.market.command;

import com.donututils.donutrep.DonutREPPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** /sellmultiplier [value] - views or (admin-only) sets the market's global sell-back fraction
 * (market.sell-back-fraction in config.yml) - how much of an item's buy price players get selling
 * it back. */
public final class SellMultiplierCommand implements CommandExecutor {

    private final DonutREPPlugin plugin;

    public SellMultiplierCommand(DonutREPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&7Current sell-back multiplier: &f" + plugin.getMarketConfig().sellBackFraction()));
            return true;
        }
        if (!sender.hasPermission("market.admin")) {
            sender.sendMessage(color("&cYou do not have permission to change that."));
            return true;
        }
        double value;
        try {
            value = Double.parseDouble(args[0]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(color("&cInvalid value."));
            return true;
        }
        plugin.getConfig().set("market.sell-back-fraction", value);
        plugin.saveConfig();
        plugin.reloadMarket();
        sender.sendMessage(color("&aSell-back multiplier set to " + value + "."));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
