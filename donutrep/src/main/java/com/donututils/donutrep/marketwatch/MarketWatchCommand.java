package com.donututils.donutrep.marketwatch;

import com.donututils.donutrep.DonutREPPlugin;
import com.donututils.donutrep.auctionhouse.AuctionHouseManager;
import com.donututils.donutrep.auctionhouse.AuctionListing;
import com.donututils.donutrep.marketwatch.discord.DiscordWebhook;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;

/**
 * /marketwatch reports total circulating Money via DonutREP's own EconomyManager. /ahstats now shows
 * real Auction House activity (highest-priced active listings) since DonutREP has a genuine
 * player-to-player marketplace (see /orders), rather than the old StockMarket-based stand-in.
 */
public final class MarketWatchCommand implements CommandExecutor {

    private final DonutREPPlugin plugin;
    private final DiscordWebhook webhook;
    private final AuctionHouseManager auctionHouse;

    public MarketWatchCommand(DonutREPPlugin plugin, DiscordWebhook webhook, AuctionHouseManager auctionHouse) {
        this.plugin = plugin;
        this.webhook = webhook;
        this.auctionHouse = auctionHouse;
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
                double total = plugin.getEconomyManager().allBalances().values().stream().mapToDouble(Double::doubleValue).sum();
                sender.sendMessage(color("&7Total circulating Money: &f$" + String.format(Locale.US, "%,.2f", total)));
                sender.sendMessage(color("&7Known players: &f" + plugin.getEconomyManager().allBalances().size()));
                sender.sendMessage(color("&7Webhook configured: " + (webhook.isConfigured() ? "&ayes" : "&cno")));
            }
            default -> sender.sendMessage(color("&cUsage: /marketwatch <reload|status>"));
        }
        return true;
    }

    public boolean onAhStats(CommandSender sender, String[] args) {
        List<AuctionListing> listings = auctionHouse.activeListings();
        if (listings.isEmpty()) {
            sender.sendMessage(color("&7The auction house has no active listings."));
            return true;
        }
        sender.sendMessage(color("&6&lAuction House Stats"));
        sender.sendMessage(color("&7Active listings: &f" + listings.size()));
        double total = listings.stream().mapToDouble(AuctionListing::price).sum();
        sender.sendMessage(color("&7Combined asking price: &f$" + String.format(Locale.US, "%,.2f", total)));
        sender.sendMessage(color("&6&lTop Listings"));
        listings.stream()
                .sorted((a, b) -> Double.compare(b.price(), a.price()))
                .limit(10)
                .forEach(listing -> sender.sendMessage(color("&e#" + listing.id() + " &f" + listing.amount() + "x "
                        + (listing.displayName() != null ? listing.displayName() : listing.material().name())
                        + " &7- $" + String.format(Locale.US, "%,.2f", listing.price()) + " &7(by " + listing.sellerName() + ")")));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
