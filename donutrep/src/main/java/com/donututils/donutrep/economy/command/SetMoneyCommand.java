package com.donututils.donutrep.economy.command;

import com.donututils.donutrep.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Locale;

/** /setmoney <player> <amount> - admin command to set a player's Money balance exactly. */
public final class SetMoneyCommand implements CommandExecutor {

    private final EconomyManager economy;

    public SetMoneyCommand(EconomyManager economy) {
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
            sender.sendMessage(color("&cUsage: /setmoney <player> <amount>"));
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
        if (amount < 0) {
            sender.sendMessage(color("&cAmount cannot be negative."));
            return true;
        }
        double current = economy.getBalance(target);
        if (amount > current) {
            economy.depositPlayer(target, amount - current);
        } else if (amount < current) {
            economy.withdrawPlayer(target, current - amount);
        }
        sender.sendMessage(color("&aSet " + args[0] + "'s balance to " + formatMoney(amount) + "."));
        return true;
    }

    private static String formatMoney(double amount) {
        return "$" + String.format(Locale.US, "%,.2f", amount);
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
