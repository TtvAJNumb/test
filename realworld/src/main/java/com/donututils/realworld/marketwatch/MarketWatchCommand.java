package com.donututils.realworld.marketwatch;

import com.donututils.realworld.RealWorldPlugin;
import com.donututils.realworld.marketwatch.discord.DiscordWebhook;
import com.donututils.realworld.stockmarket.engine.StockRegistry;
import com.donututils.realworld.stockmarket.model.Stock;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Locale;
import java.util.Map;

/**
 * Rebuilt against RealWorld's own data rather than UltimateDonutSmp's - the original MarketWatch
 * tracked total circulating money via UDS's LeaderboardManager and auction house sale prices via its
 * AuctionHouseManager. Money comes from Ledger's own balance cache here; there is no auction house in
 * RealWorld yet, so /ahstats now reports StockMarket price history instead (the closest real analog -
 * a genuinely tracked, publicly traded price series), not literal auction-house data.
 */
public final class MarketWatchCommand implements CommandExecutor {

    private final RealWorldPlugin plugin;
    private final DiscordWebhook webhook;
    private final StockRegistry stockRegistry;

    public MarketWatchCommand(RealWorldPlugin plugin, DiscordWebhook webhook, StockRegistry stockRegistry) {
        this.plugin = plugin;
        this.webhook = webhook;
        this.stockRegistry = stockRegistry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&cUsage: /marketwatch <reload|status>"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadMarketWatch();
                sender.sendMessage(color("&aMarketWatch reloaded."));
            }
            case "status" -> {
                double total = plugin.getEconomyProvider().allCheckingBalances().values().stream().mapToDouble(Double::doubleValue).sum();
                sender.sendMessage(color("&7Total circulating Money: &f$" + String.format(Locale.US, "%,.2f", total)));
                sender.sendMessage(color("&7Known players: &f" + plugin.getEconomyProvider().allCheckingBalances().size()));
                sender.sendMessage(color("&7Webhook configured: " + (webhook.isConfigured() ? "&ayes" : "&cno")));
            }
            default -> sender.sendMessage(color("&cUsage: /marketwatch <reload|status>"));
        }
        return true;
    }

    public boolean onAhStats(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&6&lMost-traded stocks"));
            stockRegistry.allActive().stream()
                    .sorted((a, b) -> Long.compare(b.volumeToday(), a.volumeToday()))
                    .limit(10)
                    .forEach(stock -> sender.sendMessage(color("&e" + stock.symbol() + " &7- $" + String.format(Locale.US, "%.2f", stock.price())
                            + " &7(" + stock.volumeToday() + " traded today)")));
            return true;
        }
        Stock stock = stockRegistry.get(args[0]);
        if (stock == null) {
            sender.sendMessage(color("&cNo stock with symbol " + args[0].toUpperCase(Locale.ROOT) + "."));
            return true;
        }
        sender.sendMessage(color("&6&l" + stock.symbol() + " &7(" + stock.name() + ")"));
        sender.sendMessage(color(String.format(Locale.US, "&7Price: &f$%.2f &7(%+.2f%% today)", stock.price(), stock.dayChangePercent())));
        sender.sendMessage(color(String.format(Locale.US, "&7Day range: &f$%.2f - $%.2f", stock.dayLow(), stock.dayHigh())));
        sender.sendMessage(color("&7Volume today: &f" + stock.volumeToday()));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
