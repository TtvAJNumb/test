package com.donututils.donutrep.market.command;

import com.donututils.donutrep.DonutREPPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** /shopedit reload - matches real UDS's actual command name for this job (DonutREP previously called
 * it /shopadmin, which isn't a real UDS command at all). Re-reads the shop categories/items/prices
 * from config.yml without a server restart. */
public final class ShopEditCommand implements CommandExecutor {

    private final DonutREPPlugin plugin;

    public ShopEditCommand(DonutREPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ultimatedonutsmp.admin.shop")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return true;
        }
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(color("&cUsage: /shopedit reload"));
            return true;
        }
        plugin.reloadMarket();
        sender.sendMessage(color("&aShop config reloaded."));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
