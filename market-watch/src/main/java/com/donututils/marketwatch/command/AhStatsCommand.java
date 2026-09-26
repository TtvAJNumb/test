package com.donututils.marketwatch.command;

import com.donututils.marketwatch.service.MarketStatsService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;

public final class AhStatsCommand implements CommandExecutor {

    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    private final MarketStatsService statsService;

    public AhStatsCommand(MarketStatsService statsService) {
        this.statsService = statsService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showTopItems(sender);
            return true;
        }

        String material = args[0].toUpperCase(Locale.ROOT);
        sender.sendMessage(legacy("&6&lAuction stats: &f" + material));
        sendWindow(sender, material, "24h", DAY_MILLIS);
        sendWindow(sender, material, "7d", DAY_MILLIS * 7);
        sendWindow(sender, material, "30d", DAY_MILLIS * 30);
        return true;
    }

    private void sendWindow(CommandSender sender, String material, String label, long windowMillis) {
        MarketStatsService.ItemStats stats = statsService.getItemStats(material, windowMillis);
        if (stats.volume() == 0) {
            sender.sendMessage(legacy("&7" + label + ": &8no sales recorded"));
            return;
        }
        sender.sendMessage(legacy(String.format(Locale.US,
                "&7%s: &favg $%,.2f &7(min $%,.2f, max $%,.2f, %d sold)",
                label, stats.avgPrice(), stats.minPrice(), stats.maxPrice(), stats.volume())));
    }

    private void showTopItems(CommandSender sender) {
        List<MarketStatsService.ItemStats> items = statsService.getTopTradedItems(DAY_MILLIS * 7, 10);
        if (items.isEmpty()) {
            sender.sendMessage(legacy("&7No auction sales recorded in the last 7 days yet."));
            return;
        }
        sender.sendMessage(legacy("&6&lMost-traded items &7(last 7 days)"));
        int rank = 1;
        for (MarketStatsService.ItemStats stats : items) {
            sender.sendMessage(legacy(String.format(Locale.US,
                    "&e#%d &f%s &7- avg $%,.2f, %d sold",
                    rank++, stats.material(), stats.avgPrice(), stats.volume())));
        }
        sender.sendMessage(legacy("&7Use &f/ahstats <item> &7for a specific item."));
    }

    private static String legacy(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
