package com.donututils.ledger.command;

import com.donututils.ledger.LedgerPlugin;
import com.donututils.ledger.loan.LoanManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Locale;

public final class LedgerAdminCommand implements CommandExecutor {

    private final LedgerPlugin plugin;
    private final LoanManager loanManager;

    public LedgerAdminCommand(LedgerPlugin plugin, LoanManager loanManager) {
        this.plugin = plugin;
        this.loanManager = loanManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&cUsage: /ledgeradmin <reload|forgiveloan> ..."));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadLedger();
                sender.sendMessage(color("&aLedger config reloaded."));
            }
            case "forgiveloan" -> forgiveLoan(sender, args);
            default -> sender.sendMessage(color("&cUsage: /ledgeradmin <reload|forgiveloan> ..."));
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private void forgiveLoan(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /ledgeradmin forgiveloan <player>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        boolean forgiven = loanManager.forgiveLoan(target.getUniqueId());
        sender.sendMessage(color(forgiven
                ? "&aForgave " + args[1] + "'s outstanding loan."
                : "&c" + args[1] + " doesn't have an active loan."));
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
