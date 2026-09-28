package com.donututils.realworld.stockmarket.command;

import com.donututils.realworld.stockmarket.gui.PagedMenu;
import com.donututils.realworld.stockmarket.service.PortfolioService;
import com.donututils.realworld.stockmarket.storage.PortfolioStore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class StockLeaderboardCommand implements CommandExecutor {

    private static final int TOP_N = 10;

    private final PortfolioStore portfolioStore;
    private final PortfolioService portfolioService;

    public StockLeaderboardCommand(PortfolioStore portfolioStore, PortfolioService portfolioService) {
        this.portfolioStore = portfolioStore;
        this.portfolioService = portfolioService;
    }

    private record Entry(UUID uuid, double netWorth) {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<Entry> entries = new ArrayList<>();
        for (UUID uuid : portfolioStore.listAllPlayerIds()) {
            entries.add(new Entry(uuid, portfolioService.totalNetWorth(uuid)));
        }
        entries.sort(Comparator.comparingDouble(Entry::netWorth).reversed());

        if (entries.isEmpty()) {
            sender.sendMessage(PagedMenu.legacy("&7No one has a portfolio yet."));
            return true;
        }

        sender.sendMessage(PagedMenu.legacy("&6&lTop stock market portfolios"));
        int rank = 1;
        for (Entry entry : entries) {
            if (rank > TOP_N) {
                break;
            }
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.uuid());
            String name = player.getName() != null ? player.getName() : entry.uuid().toString().substring(0, 8);
            sender.sendMessage(PagedMenu.legacy(String.format(Locale.US,
                    "&e#%d &f%s &7- &a$%,.2f", rank++, name, entry.netWorth())));
        }
        return true;
    }
}
