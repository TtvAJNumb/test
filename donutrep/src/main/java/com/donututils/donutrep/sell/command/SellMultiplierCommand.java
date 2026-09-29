package com.donututils.donutrep.sell.command;

import com.donututils.donutrep.DonutREPPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Locale;

/** /sellmultiplier [value] - view, or (admin) set, the global sell-price multiplier applied on top of
 * worth.yml's base prices. */
public final class SellMultiplierCommand implements CommandExecutor {

    private final DonutREPPlugin plugin;

    public SellMultiplierCommand(DonutREPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&7Current sell multiplier: &f" + plugin.getSellMultiplier() + "x"));
            return true;
        }
        if (!sender.hasPermission("sell.admin")) {
            sender.sendMessage(color("&cYou do not have permission to change the sell multiplier."));
            return true;
        }
        double value;
        try {
            value = Double.parseDouble(args[0]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(color("&cInvalid value."));
            return true;
        }
        plugin.getConfig().set("sell.multiplier", value);
        plugin.saveConfig();
        plugin.reloadSell();
        sender.sendMessage(color("&aSell multiplier set to " + value + "x."));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
