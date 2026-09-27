package com.donututils.stockmarket.command;

import com.donututils.stockmarket.engine.StockRegistry;
import com.donututils.stockmarket.gui.PagedMenu;
import com.donututils.stockmarket.gui.StockMenus;
import com.donututils.stockmarket.model.Stock;
import com.donututils.stockmarket.service.TradingService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class StockCommand implements CommandExecutor {

    private final StockRegistry registry;
    private final TradingService tradingService;
    private final StockMenus menus;

    public StockCommand(StockRegistry registry, TradingService tradingService, StockMenus menus) {
        this.registry = registry;
        this.tradingService = tradingService;
        this.menus = menus;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            menus.openMarket(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "price" -> handlePrice(player, args);
            case "buy" -> handleTrade(player, args, true);
            case "sell" -> handleTrade(player, args, false);
            default -> sendUsage(player);
        }
        return true;
    }

    private void handlePrice(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(PagedMenu.legacy("&cUsage: /stocks price <symbol>"));
            return;
        }
        Stock stock = registry.get(args[1]);
        if (stock == null) {
            player.sendMessage(PagedMenu.legacy("&cUnknown stock: " + args[1]));
            return;
        }
        player.sendMessage(PagedMenu.legacy(String.format(Locale.US,
                "&6%s &f(%s) &7- &f$%,.2f &7(%+.2f%% today)",
                stock.symbol(), stock.name(), stock.price(), stock.dayChangePercent())));
    }

    private void handleTrade(Player player, String[] args, boolean buy) {
        if (args.length < 3) {
            player.sendMessage(PagedMenu.legacy("&cUsage: /stocks " + (buy ? "buy" : "sell") + " <symbol> <shares>"));
            return;
        }
        long shares;
        try {
            shares = Long.parseLong(args[2]);
        } catch (NumberFormatException ex) {
            player.sendMessage(PagedMenu.legacy("&cShares must be a whole number."));
            return;
        }

        TradingService.TradeResult result = buy
                ? tradingService.buy(player.getUniqueId(), player.getName(), args[1], shares)
                : tradingService.sell(player.getUniqueId(), player.getName(), args[1], shares);

        player.sendMessage(PagedMenu.legacy((result.success() ? "&a" : "&c") + result.message()));
    }

    private void sendUsage(Player player) {
        player.sendMessage(PagedMenu.legacy("&cUsage: /stocks [price|buy|sell] ..."));
    }
}
