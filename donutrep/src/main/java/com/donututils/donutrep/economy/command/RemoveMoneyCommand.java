package com.donututils.donutrep.economy.command;

import com.donututils.donutrep.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Locale;

/** /removemoney <player> <amount> - admin command; clamps to the player's current balance rather
 * than failing, same as /removeshards. */
public final class RemoveMoneyCommand implements CommandExecutor {

    private final EconomyManager economy;

    public RemoveMoneyCommand(EconomyManager economy) {
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
            sender.sendMessage(color("&cUsage: /removemoney <player> <amount>"));
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
        double current = economy.getBalance(target);
        double toRemove = Math.min(current, amount);
        if (toRemove > 0) {
            economy.withdrawPlayer(target, toRemove);
        }
        sender.sendMessage(color("&aRemoved " + formatMoney(toRemove) + " from " + args[0] + "."));
        return true;
    }

    private static String formatMoney(double amount) {
        return "$" + String.format(Locale.US, "%,.2f", amount);
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
