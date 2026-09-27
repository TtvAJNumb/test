package com.donututils.stockmarket.gui;

import com.donututils.stockmarket.engine.StockRegistry;
import com.donututils.stockmarket.listener.ChatQuantityPrompt;
import com.donututils.stockmarket.model.Stock;
import com.donututils.stockmarket.service.PortfolioService;
import com.donututils.stockmarket.service.TradingService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Owns navigation between the market browser, stock detail, and portfolio menus. */
public final class StockMenus {

    private final StockRegistry registry;
    private final TradingService tradingService;
    private final PortfolioService portfolioService;
    private final ChatQuantityPrompt quantityPrompt;

    public StockMenus(StockRegistry registry, TradingService tradingService, PortfolioService portfolioService,
                       ChatQuantityPrompt quantityPrompt) {
        this.registry = registry;
        this.tradingService = tradingService;
        this.portfolioService = portfolioService;
        this.quantityPrompt = quantityPrompt;
    }

    public void openMarket(Player player) {
        List<Stock> stocks = new ArrayList<>(registry.allActive());
        stocks.sort(Comparator.comparing(Stock::symbol));

        List<ItemStack> items = new ArrayList<>();
        List<Consumer<Player>> actions = new ArrayList<>();
        for (Stock stock : stocks) {
            items.add(marketItem(stock));
            actions.add(p -> openStockDetail(p, stock.symbol()));
        }
        if (items.isEmpty()) {
            items.add(item(Material.BARRIER, "&7No stocks listed yet", List.of()));
            actions.add(null);
        }

        PagedMenu.open(player, new StockMenuHolder(), "&8Stock Market", items, actions, null);
    }

    public void openStockDetail(Player player, String symbol) {
        Stock stock = registry.get(symbol);
        if (stock == null) {
            player.sendMessage(PagedMenu.legacy("&cThat stock no longer exists."));
            openMarket(player);
            return;
        }

        List<ItemStack> items = new ArrayList<>();
        List<Consumer<Player>> actions = new ArrayList<>();

        items.add(detailItem(stock));
        actions.add(null);

        items.add(item(Material.LIME_STAINED_GLASS_PANE, "&aBuy", List.of("&7Click to buy shares of " + stock.symbol() + ".")));
        actions.add(p -> promptBuy(p, stock.symbol()));

        items.add(item(Material.RED_STAINED_GLASS_PANE, "&cSell", List.of("&7Click to sell shares of " + stock.symbol() + ".")));
        actions.add(p -> promptSell(p, stock.symbol()));

        PagedMenu.open(player, new StockMenuHolder(), "&8" + stock.symbol(), items, actions, this::openMarket);
    }

    public void openPortfolio(Player player) {
        PortfolioService.PortfolioView view = portfolioService.getPortfolio(player.getUniqueId());

        List<ItemStack> items = new ArrayList<>();
        List<Consumer<Player>> actions = new ArrayList<>();

        items.add(item(Material.GOLD_INGOT, "&6Summary", List.of(
                "&7Cash: &f$" + fmt(view.cash()),
                "&7Holdings value: &f$" + fmt(view.holdingsValue()),
                "&7Total net worth: &a$" + fmt(view.totalValue())
        )));
        actions.add(null);

        List<PortfolioService.PositionView> positions = new ArrayList<>(view.positions());
        positions.sort(Comparator.comparing(PortfolioService.PositionView::symbol));
        for (PortfolioService.PositionView position : positions) {
            items.add(positionItem(position));
            actions.add(p -> openStockDetail(p, position.symbol()));
        }

        PagedMenu.open(player, new StockMenuHolder(), "&8Your Portfolio", items, actions, null);
    }

    private void promptBuy(Player player, String symbol) {
        player.closeInventory();
        quantityPrompt.prompt(player, "&aHow many shares of " + symbol + " do you want to buy?", qty -> {
            TradingService.TradeResult result = tradingService.buy(player.getUniqueId(), player.getName(), symbol, qty);
            player.sendMessage(PagedMenu.legacy((result.success() ? "&a" : "&c") + result.message()));
            openStockDetail(player, symbol);
        });
    }

    private void promptSell(Player player, String symbol) {
        player.closeInventory();
        quantityPrompt.prompt(player, "&cHow many shares of " + symbol + " do you want to sell?", qty -> {
            TradingService.TradeResult result = tradingService.sell(player.getUniqueId(), player.getName(), symbol, qty);
            player.sendMessage(PagedMenu.legacy((result.success() ? "&a" : "&c") + result.message()));
            openStockDetail(player, symbol);
        });
    }

    private ItemStack marketItem(Stock stock) {
        double changePercent = stock.dayChangePercent();
        String changeColor = changePercent >= 0 ? "&a" : "&c";
        List<String> lore = new ArrayList<>();
        lore.add("&7Price: &f$" + fmt(stock.price()));
        lore.add("&7Today: " + changeColor + fmtPercent(changePercent));
        lore.add("&7Sector: &f" + stock.sector());
        if (stock.halted()) {
            lore.add("&c⚠ Trading halted");
        }
        return item(Material.PAPER, (changePercent >= 0 ? "&a" : "&c") + stock.symbol() + " &f- " + stock.name(), lore);
    }

    private ItemStack detailItem(Stock stock) {
        List<String> lore = new ArrayList<>();
        lore.add("&7" + stock.name() + " (" + stock.sector() + ")");
        lore.add("");
        lore.add("&7Price: &f$" + fmt(stock.price()));
        lore.add("&7Today: " + (stock.dayChangePercent() >= 0 ? "&a" : "&c") + fmtPercent(stock.dayChangePercent()));
        lore.add("&7Day range: &f$" + fmt(stock.dayLow()) + " - $" + fmt(stock.dayHigh()));
        lore.add("&7Volume today: &f" + stock.volumeToday());
        lore.add("&7Market cap: &f$" + fmt(stock.marketCap()));
        if (stock.paysDividends()) {
            lore.add("&7Dividend: &f" + fmtPercent(stock.dividendYieldPerPayout() * 100) + " per payout");
        }
        if (stock.halted()) {
            lore.add("&c⚠ Trading halted");
        }
        return item(Material.NETHER_STAR, "&6" + stock.symbol(), lore);
    }

    private ItemStack positionItem(PortfolioService.PositionView position) {
        List<String> lore = List.of(
                "&7Shares: &f" + position.shares(),
                "&7Avg cost: &f$" + fmt(position.averageCost()),
                "&7Current price: &f$" + fmt(position.currentPrice()),
                "&7Market value: &f$" + fmt(position.marketValue()),
                "&7Unrealized P&L: " + (position.unrealizedPnl() >= 0 ? "&a" : "&c") + "$" + fmt(position.unrealizedPnl())
                        + " (" + fmtPercent(position.unrealizedPnlPercent()) + ")"
        );
        return item(Material.PLAYER_HEAD, "&6" + position.symbol(), lore);
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(PagedMenu.legacy(name));
            List<String> colored = new ArrayList<>();
            for (String line : lore) {
                colored.add(PagedMenu.legacy(line));
            }
            meta.setLore(colored);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }

    private static String fmtPercent(double value) {
        return String.format(Locale.US, "%+.2f%%", value);
    }
}
