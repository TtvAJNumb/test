package com.donututils.realworld.ledger.command;

import com.donututils.realworld.RealWorldPlugin;
import com.donututils.realworld.ledger.bank.BankManager;
import com.donututils.realworld.ledger.credit.CreditScoreManager;
import com.donututils.realworld.ledger.economy.LedgerEconomyProvider;
import com.donututils.realworld.ledger.model.CreditProfile;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class BankCommand implements CommandExecutor {

    private final RealWorldPlugin plugin;
    private final LedgerEconomyProvider economy;
    private final BankManager bankManager;
    private final CreditScoreManager creditScoreManager;

    public BankCommand(RealWorldPlugin plugin, LedgerEconomyProvider economy, BankManager bankManager,
                        CreditScoreManager creditScoreManager) {
        this.plugin = plugin;
        this.economy = economy;
        this.bankManager = bankManager;
        this.creditScoreManager = creditScoreManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can use the bank."));
            return true;
        }

        if (args.length == 0) {
            showBalance(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "balance" -> showBalance(player);
            case "credit" -> showCredit(player);
            case "deposit" -> deposit(player, args);
            case "withdraw" -> withdraw(player, args);
            default -> player.sendMessage(color("&cUsage: /bank [balance|credit|deposit <amount>|withdraw <amount>]"));
        }
        return true;
    }

    private void showBalance(Player player) {
        double checking = economy.getBalance(player);
        double savings = bankManager.getSavingsBalance(player.getUniqueId());
        player.sendMessage(color("&6&lYour Accounts"));
        player.sendMessage(color("&7Checking: &f" + economy.format(checking)));
        player.sendMessage(color("&7Savings: &f" + economy.format(savings) + " &7(earning interest)"));
        player.sendMessage(color("&7Total: &a" + economy.format(checking + savings)));
    }

    private void showCredit(Player player) {
        CreditProfile profile = creditScoreManager.getCached(player.getUniqueId());
        player.sendMessage(color("&6&lCredit Report"));
        player.sendMessage(color("&7Score: &f" + profile.score() + " &7(300-850)"));
        player.sendMessage(color("&7On-time payments: &a" + profile.onTimePayments()));
        player.sendMessage(color("&7Missed payments: &c" + profile.missedPayments()));
        player.sendMessage(color("&7Defaults: &4" + profile.defaults()));
    }

    private void deposit(Player player, String[] args) {
        Double amount = parseAmount(player, args);
        if (amount == null) {
            return;
        }
        BankManager.TransferResult result = bankManager.depositToSavings(player, amount);
        player.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
    }

    private void withdraw(Player player, String[] args) {
        Double amount = parseAmount(player, args);
        if (amount == null) {
            return;
        }
        BankManager.TransferResult result = bankManager.withdrawFromSavings(player, amount);
        player.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
    }

    private Double parseAmount(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /bank " + args[0].toLowerCase(Locale.ROOT) + " <amount>"));
            return null;
        }
        try {
            return Double.parseDouble(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(color("&cInvalid amount."));
            return null;
        }
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
