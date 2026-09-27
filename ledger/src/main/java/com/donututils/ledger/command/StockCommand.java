package com.donututils.ledger.command;

import com.donututils.ledger.LedgerPlugin;
import com.donututils.ledger.corp.CorporationManager;
import com.donututils.ledger.economy.LedgerEconomyProvider;
import com.donututils.ledger.model.Corporation;
import com.donututils.ledger.stock.ShareTradingManager;
import com.donututils.ledger.tax.TaxManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class StockCommand implements CommandExecutor {

    private final LedgerPlugin plugin;
    private final CorporationManager corporationManager;
    private final ShareTradingManager shareTradingManager;
    private final TaxManager taxManager;
    private final LedgerEconomyProvider economy;

    public StockCommand(LedgerPlugin plugin, CorporationManager corporationManager, ShareTradingManager shareTradingManager,
                         TaxManager taxManager, LedgerEconomyProvider economy) {
        this.plugin = plugin;
        this.corporationManager = corporationManager;
        this.shareTradingManager = shareTradingManager;
        this.taxManager = taxManager;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
            listCorps(sender);
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can trade shares."));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(color("&cUsage: /stock [buy <ticker> <qty>|sell <ticker> <qty>|list|portfolio]"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "buy" -> buy(player, args);
            case "sell" -> sell(player, args);
            case "portfolio" -> portfolio(player);
            default -> player.sendMessage(color("&cUsage: /stock [buy <ticker> <qty>|sell <ticker> <qty>|list|portfolio]"));
        }
        return true;
    }

    private void buy(Player player, String[] args) {
        Corporation corp = requireCorp(player, args);
        if (corp == null) {
            return;
        }
        Integer quantity = parseQuantity(player, args);
        if (quantity == null) {
            return;
        }
        double grossCost = corp.sharePrice() * quantity;
        ShareTradingManager.TradeResult result = shareTradingManager.buy(player, corp, quantity);
        if (!result.success()) {
            player.sendMessage(color("&c" + result.message()));
            return;
        }
        double tax = taxManager.applyTransactionTax(player, grossCost);
        player.sendMessage(color("&a" + result.message() + (tax > 0 ? " &7(+" + economy.format(tax) + " tax)" : "")));
    }

    private void sell(Player player, String[] args) {
        Corporation corp = requireCorp(player, args);
        if (corp == null) {
            return;
        }
        Integer quantity = parseQuantity(player, args);
        if (quantity == null) {
            return;
        }
        double grossProceeds = corp.sharePrice() * quantity;
        ShareTradingManager.TradeResult result = shareTradingManager.sell(player, corp, quantity);
        if (!result.success()) {
            player.sendMessage(color("&c" + result.message()));
            return;
        }
        double tax = taxManager.applyTransactionTax(player, grossProceeds);
        player.sendMessage(color("&a" + result.message() + (tax > 0 ? " &7(-" + economy.format(tax) + " tax)" : "")));
    }

    private void listCorps(CommandSender sender) {
        if (corporationManager.all().isEmpty()) {
            sender.sendMessage(color("&7No corporations have been founded yet - see /corp found."));
            return;
        }
        sender.sendMessage(color("&6&lCorporations"));
        for (Corporation corp : corporationManager.all()) {
            sender.sendMessage(color(String.format(Locale.US, "&e%s &7- %s: &f$%,.2f/share", corp.ticker(), corp.name(), corp.sharePrice())));
        }
    }

    private void portfolio(Player player) {
        boolean any = false;
        player.sendMessage(color("&6&lYour Portfolio"));
        for (Corporation corp : corporationManager.all()) {
            int shares = corporationManager.sharesHeldBy(corp.id(), player.getUniqueId());
            if (shares > 0) {
                any = true;
                double value = shares * corp.sharePrice();
                player.sendMessage(color(String.format(Locale.US, "&e%s&7: &f%d shares &7(%s)", corp.ticker(), shares, economy.format(value))));
            }
        }
        if (!any) {
            player.sendMessage(color("&7You don't own any shares yet."));
        }
    }

    private Corporation requireCorp(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /stock " + args[0].toLowerCase(Locale.ROOT) + " <ticker> <qty>"));
            return null;
        }
        Corporation corp = corporationManager.getByTicker(args[1]);
        if (corp == null) {
            player.sendMessage(color("&cUnknown corporation ticker: " + args[1].toUpperCase(Locale.ROOT)));
        }
        return corp;
    }

    private Integer parseQuantity(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(color("&cUsage: /stock " + args[0].toLowerCase(Locale.ROOT) + " <ticker> <qty>"));
            return null;
        }
        try {
            return Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            player.sendMessage(color("&cInvalid quantity."));
            return null;
        }
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
