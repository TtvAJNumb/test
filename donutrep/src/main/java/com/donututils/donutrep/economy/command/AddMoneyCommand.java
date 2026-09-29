package com.donututils.donutrep.economy.command;

import com.donututils.donutrep.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Locale;

/** /addmoney <player> <amount> - admin command to grant Money directly. */
public final class AddMoneyCommand implements CommandExecutor {

    private final EconomyManager economy;

    public AddMoneyCommand(EconomyManager economy) {
        this.economy = economy;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("economy.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /addmoney <player> <amount>"));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(color("&cInvalid amount."));
            return true;
        }
        if (amount <= 0) {
            sender.sendMessage(color("&cAmount must be positive."));
            return true;
        }
        economy.depositPlayer(target, amount);
        sender.sendMessage(color("&aGave " + formatMoney(amount) + " to " + args[0] + "."));
        return true;
    }

    private static String formatMoney(double amount) {
        return "$" + String.format(Locale.US, "%,.2f", amount);
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
