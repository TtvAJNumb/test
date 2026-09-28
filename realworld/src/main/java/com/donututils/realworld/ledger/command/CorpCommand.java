package com.donututils.realworld.ledger.command;

import com.donututils.realworld.RealWorldPlugin;
import com.donututils.realworld.ledger.corp.CorporationManager;
import com.donututils.realworld.ledger.model.Corporation;
import com.donututils.realworld.ledger.stock.ShareTradingManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class CorpCommand implements CommandExecutor {

    private final RealWorldPlugin plugin;
    private final CorporationManager corporationManager;
    private final ShareTradingManager shareTradingManager;

    public CorpCommand(RealWorldPlugin plugin, CorporationManager corporationManager, ShareTradingManager shareTradingManager) {
        this.plugin = plugin;
        this.corporationManager = corporationManager;
        this.shareTradingManager = shareTradingManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can manage corporations."));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(color("&cUsage: /corp [found <name> <ticker> [sector]|report <ticker> <revenue> <expenses>|info <ticker>|dividend <ticker> <amount>|trust <ticker> <on|off>]"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "found" -> found(player, args);
            case "report" -> report(player, args);
            case "info" -> info(player, args);
            case "dividend" -> dividend(player, args);
            case "trust" -> trust(player, args);
            default -> player.sendMessage(color("&cUnknown /corp subcommand."));
        }
        return true;
    }

    private void found(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(color("&cUsage: /corp found <name> <ticker> [sector]"));
            return;
        }
        String name = args[1];
        String ticker = args[2];
        String sector = args.length >= 4 ? args[3] : null;

        // CorporationManager#found blocks on a database insert to get the corporation's id back -
        // never call it directly from a command handler on the main thread.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            CorporationManager.CorpResult result = corporationManager.found(player, name, ticker, sector);
            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(color((result.success() ? "&a" : "&c") + result.message())));
        });
    }

    private void report(Player player, String[] args) {
        Corporation corp = requireCorp(player, args, 1);
        if (corp == null) {
            return;
        }
        if (args.length < 4) {
            player.sendMessage(color("&cUsage: /corp report <ticker> <revenue> <expenses>"));
            return;
        }
        double revenue;
        double expenses;
        try {
            revenue = Double.parseDouble(args[2]);
            expenses = Double.parseDouble(args[3]);
        } catch (NumberFormatException ex) {
            player.sendMessage(color("&cInvalid revenue/expenses."));
            return;
        }
        CorporationManager.CorpResult result = corporationManager.reportFinancials(player, corp, revenue, expenses);
        player.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
    }

    private void info(Player player, String[] args) {
        Corporation corp = requireCorp(player, args, 1);
        if (corp == null) {
            return;
        }
        player.sendMessage(color("&6&l" + corp.name() + " &7(" + corp.ticker() + ")"));
        player.sendMessage(color("&7Sector: &f" + corp.sector()));
        player.sendMessage(color(String.format(Locale.US, "&7Share price: &f$%,.2f &7(%d shares, $%,.2f market cap)", corp.sharePrice(), corp.totalShares(), corp.marketCap())));
        player.sendMessage(color(String.format(Locale.US, "&7Treasury: &f$%,.2f", corp.treasuryBalance())));
        player.sendMessage(color("&7Trust protected: " + (corp.trustProtected() ? "&aYes" : "&7No")));
        int yourShares = corporationManager.sharesHeldBy(corp.id(), player.getUniqueId());
        if (yourShares > 0) {
            player.sendMessage(color(String.format(Locale.US, "&7Your shares: &f%d &7(%.1f%%)", yourShares, yourShares * 100.0 / corp.totalShares())));
        }
    }

    private void dividend(Player player, String[] args) {
        Corporation corp = requireCorp(player, args, 1);
        if (corp == null) {
            return;
        }
        if (args.length < 3) {
            player.sendMessage(color("&cUsage: /corp dividend <ticker> <amount>"));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException ex) {
            player.sendMessage(color("&cInvalid amount."));
            return;
        }
        ShareTradingManager.TradeResult result = shareTradingManager.payDividend(player, corp, amount);
        player.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
    }

    private void trust(Player player, String[] args) {
        Corporation corp = requireCorp(player, args, 1);
        if (corp == null) {
            return;
        }
        if (args.length < 3 || (!args[2].equalsIgnoreCase("on") && !args[2].equalsIgnoreCase("off"))) {
            player.sendMessage(color("&cUsage: /corp trust <ticker> <on|off>"));
            return;
        }
        CorporationManager.CorpResult result = corporationManager.setTrust(player, corp, args[2].equalsIgnoreCase("on"));
        player.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
    }

    private Corporation requireCorp(Player player, String[] args, int index) {
        if (args.length <= index) {
            player.sendMessage(color("&cYou need to specify a ticker."));
            return null;
        }
        Corporation corp = corporationManager.getByTicker(args[index]);
        if (corp == null) {
            player.sendMessage(color("&cUnknown corporation ticker: " + args[index].toUpperCase(Locale.ROOT)));
        }
        return corp;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
