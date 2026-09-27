package com.donututils.aichat.tool;

import com.donututils.aichat.json.MiniJson;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Locale;
import java.util.Map;

/**
 * Read-only lookups against StockMarket's stocks.yml. There's no compile-time dependency on that
 * plugin - this just reads the same public, non-sensitive price data StockMarket itself persists to
 * disk, so it works whether or not StockMarket happens to be on the classpath.
 */
final class StockMarketContext {

    private StockMarketContext() {
    }

    @SuppressWarnings("unchecked")
    static void registerTools(ServerContextService service) {
        service.register(new ToolDefinition(
                "get_stock_price",
                (Map<String, Object>) MiniJson.parse("""
                        {"name":"get_stock_price","description":"Get the current price, sector, and today's change for one stock on this server's StockMarket plugin, by ticker symbol.","input_schema":{"type":"object","properties":{"symbol":{"type":"string","description":"The stock ticker symbol, e.g. TSM"}},"required":["symbol"]}}
                        """),
                (args, sender) -> getStockPrice(args.get("symbol"))
        ));
        service.register(new ToolDefinition(
                "list_stocks",
                (Map<String, Object>) MiniJson.parse("""
                        {"name":"list_stocks","description":"List every active stock currently on this server's StockMarket, with its current price.","input_schema":{"type":"object","properties":{}}}
                        """),
                (args, sender) -> listStocks()
        ));
    }

    private static File stocksFile() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("StockMarket");
        if (plugin == null) {
            return null;
        }
        return new File(plugin.getDataFolder(), "stocks.yml");
    }

    private static String getStockPrice(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return "No stock symbol was given.";
        }
        File file = stocksFile();
        if (file == null || !file.exists()) {
            return "StockMarket has no stock data yet.";
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("stocks." + symbol.toUpperCase(Locale.ROOT));
        if (section == null) {
            return "No stock with symbol " + symbol.toUpperCase(Locale.ROOT) + " exists on this server.";
        }
        double price = section.getDouble("price", 0);
        double previousClose = section.getDouble("previousClose", price);
        double changePercent = previousClose == 0 ? 0 : ((price - previousClose) / previousClose) * 100.0;
        boolean halted = section.getBoolean("halted", false);
        boolean delisted = section.getBoolean("delisted", false);
        return String.format(Locale.US,
                "%s (%s), sector %s: $%.2f, %+.2f%% today.%s%s",
                symbol.toUpperCase(Locale.ROOT), section.getString("name", symbol), section.getString("sector", "General"),
                price, changePercent,
                halted ? " Trading is currently halted." : "",
                delisted ? " This stock has been delisted." : "");
    }

    private static String listStocks() {
        File file = stocksFile();
        if (file == null || !file.exists()) {
            return "StockMarket has no stock data yet.";
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection stocksSection = yaml.getConfigurationSection("stocks");
        if (stocksSection == null || stocksSection.getKeys(false).isEmpty()) {
            return "There are no stocks listed on this server yet.";
        }
        StringBuilder out = new StringBuilder();
        for (String symbol : stocksSection.getKeys(false)) {
            ConfigurationSection s = stocksSection.getConfigurationSection(symbol);
            if (s == null || s.getBoolean("delisted", false)) {
                continue;
            }
            if (out.length() > 0) {
                out.append("; ");
            }
            out.append(symbol).append(" $").append(String.format(Locale.US, "%.2f", s.getDouble("price", 0)));
        }
        return out.length() == 0 ? "There are no active stocks listed on this server." : out.toString();
    }
}
