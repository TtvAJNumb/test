package com.donututils.donutrep.economy.command;

import com.donututils.donutrep.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/** Standard player-to-player Money transfer - the one everyday economy command Ledger was missing.
 * Also what EconomyWatchdog's large-transfer alert watches. */
public final class PayCommand implements CommandExecutor {

    private final EconomyManager economy;

    public PayCommand(EconomyManager economy) {
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /pay <player> <amount>"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(color("&c" + args[0] + " isn't online."));
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(color("&cYou can't pay yourself."));
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(color("&cInvalid amount."));
            return true;
        }
        if (amount <= 0) {
            player.sendMessage(color("&cAmount must be positive."));
            return true;
        }
        if (!economy.has(player, amount)) {
            player.sendMessage(color("&cYou don't have " + formatMoney(amount) + "."));
            return true;
        }
        economy.withdrawPlayer(player, amount);
        economy.depositPlayer(target, amount);
        player.sendMessage(color("&aSent " + formatMoney(amount) + " to " + target.getName() + "."));
        target.sendMessage(color("&a" + player.getName() + " sent you " + formatMoney(amount) + "."));
        return true;
    }

    private static String formatMoney(double amount) {
        return "$" + String.format(Locale.US, "%,.2f", amount);
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
