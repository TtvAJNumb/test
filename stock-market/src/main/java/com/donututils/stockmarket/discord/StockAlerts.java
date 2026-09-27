package com.donututils.stockmarket.discord;

import com.donututils.stockmarket.config.StockMarketConfig;
import com.donututils.stockmarket.model.Stock;

import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/** Centralizes what gets posted to Discord and under which config toggle. */
public final class StockAlerts {

    private final DiscordWebhook webhook;
    private final Supplier<StockMarketConfig> configSupplier;

    public StockAlerts(DiscordWebhook webhook, Supplier<StockMarketConfig> configSupplier) {
        this.webhook = webhook;
        this.configSupplier = configSupplier;
    }

    public void bigMover(Stock stock, double changePercent) {
        if (!configSupplier.get().alertBigMover()) {
            return;
        }
        boolean up = changePercent >= 0;
        webhook.sendAlert(
                (up ? "📈 " : "📉 ") + stock.symbol() + " " + (up ? "surges" : "drops"),
                stock.name() + " (" + stock.symbol() + ") moved " + fmtPercent(changePercent) + " to $" + fmt(stock.price()) + ".",
                up ? 0x57F287 : 0xED4245,
                Map.of("Symbol", stock.symbol(), "Price", "$" + fmt(stock.price()), "Change", fmtPercent(changePercent))
        );
    }

    public void circuitBreaker(Stock stock, double attemptedChangePercent) {
        if (!configSupplier.get().alertCircuitBreaker()) {
            return;
        }
        webhook.sendAlert(
                "⚠️ Trading halted: " + stock.symbol(),
                stock.name() + " (" + stock.symbol() + ") swung " + fmtPercent(attemptedChangePercent)
                        + " in one tick and has been halted.",
                0xFEE75C,
                Map.of("Symbol", stock.symbol(), "Attempted move", fmtPercent(attemptedChangePercent))
        );
    }

    public void ipo(Stock stock) {
        if (!configSupplier.get().alertIpo()) {
            return;
        }
        webhook.sendAlert(
                "🔔 New IPO: " + stock.symbol(),
                stock.name() + " (" + stock.symbol() + ") just listed at $" + fmt(stock.price()) + " per share.",
                0x5865F2,
                Map.of("Symbol", stock.symbol(), "Starting price", "$" + fmt(stock.price()), "Sector", stock.sector())
        );
    }

    public void split(Stock stock, double ratio) {
        if (!configSupplier.get().alertSplit()) {
            return;
        }
        webhook.sendAlert(
                "🔀 Stock split: " + stock.symbol(),
                stock.name() + " (" + stock.symbol() + ") split " + fmt(ratio) + ":1 - new price $" + fmt(stock.price()) + ".",
                0x5865F2,
                Map.of("Symbol", stock.symbol(), "Ratio", fmt(ratio) + ":1", "New price", "$" + fmt(stock.price()))
        );
    }

    public void delisted(Stock stock) {
        if (!configSupplier.get().alertDelisting()) {
            return;
        }
        webhook.sendAlert(
                "❌ Delisted: " + stock.symbol(),
                stock.name() + " (" + stock.symbol() + ") has been delisted.",
                0xED4245,
                Map.of("Symbol", stock.symbol(), "Final price", "$" + fmt(stock.price()))
        );
    }

    public void dailySummary(double indexLevel, double indexChangePercent, String topGainer, String topLoser) {
        webhook.sendAlert(
                "📊 Daily market summary",
                "Market index: " + fmt(indexLevel) + " (" + fmtPercent(indexChangePercent) + ")",
                0x2ECC71,
                Map.of("Top gainer", topGainer == null ? "n/a" : topGainer, "Top loser", topLoser == null ? "n/a" : topLoser)
        );
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }

    private static String fmtPercent(double value) {
        return String.format(Locale.US, "%+.2f%%", value);
    }
}
