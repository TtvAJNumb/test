package com.donututils.ledger.command;

import com.donututils.ledger.LedgerPlugin;
import com.donututils.ledger.loan.LoanManager;
import com.donututils.ledger.model.Loan;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class LoanCommand implements CommandExecutor {

    private final LedgerPlugin plugin;
    private final LoanManager loanManager;

    public LoanCommand(LedgerPlugin plugin, LoanManager loanManager) {
        this.plugin = plugin;
        this.loanManager = loanManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can use loans."));
            return true;
        }
        if (args.length == 0) {
            status(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "request" -> request(player, args);
            case "repay" -> repay(player, args);
            case "status" -> status(player);
            default -> player.sendMessage(color("&cUsage: /loan [request <amount>|repay <amount>|status]"));
        }
        return true;
    }

    private void request(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /loan request <amount>"));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(color("&cInvalid amount."));
            return;
        }
        // LoanManager#originate blocks on a database insert to get the loan's id back - never call it
        // directly from a command handler on the main thread.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            LoanManager.LoanResult result = loanManager.originate(player, amount);
            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(color((result.success() ? "&a" : "&c") + result.message())));
        });
    }

    private void repay(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /loan repay <amount>"));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(color("&cInvalid amount."));
            return;
        }
        LoanManager.LoanResult result = loanManager.repay(player, amount);
        player.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
    }

    private void status(Player player) {
        Loan loan = loanManager.getActiveLoan(player.getUniqueId());
        if (loan == null) {
            player.sendMessage(color("&7You don't have an active loan."));
            return;
        }
        player.sendMessage(color("&6&lActive Loan"));
        player.sendMessage(color(String.format(Locale.US, "&7Remaining balance: &f$%,.2f &7(of $%,.2f borrowed)", loan.remainingBalance(), loan.principal())));
        player.sendMessage(color(String.format(Locale.US, "&7APR: &f%.1f%%", loan.aprPercent())));
        player.sendMessage(color(String.format(Locale.US, "&7Missed payments: &c%d", loan.missedPayments())));
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
