package com.donututils.donutrep.economy.command;

import com.donututils.donutrep.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/** /balance [player] (aliases /bal, /money) - checks your own, or another player's, Money balance. */
public final class BalanceCommand implements CommandExecutor {

    private final EconomyManager economy;

    public BalanceCommand(EconomyManager economy) {
        this.economy = economy;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        OfflinePlayer target;
        if (args.length >= 1) {
            target = Bukkit.getOfflinePlayer(args[0]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(color("&cUsage: /balance <player>"));
            return true;
        }
        double balance = economy.getBalance(target);
        String whose = args.length >= 1 ? target.getName() + "'s" : "Your";
        sender.sendMessage(color("&a" + whose + " balance: &f$" + String.format(Locale.US, "%,.2f", balance)));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
