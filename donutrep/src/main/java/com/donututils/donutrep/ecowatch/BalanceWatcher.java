package com.donututils.donutrep.ecowatch;

import com.donututils.donutrep.ecowatch.discord.DiscordWebhook;
import com.donututils.donutrep.economy.EconomyManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Periodically snapshots every known player's checking balance and alerts on a jump bigger than
 * the configured threshold since the last snapshot - the same idea as the original EconomyWatchdog,
 * just reading Ledger's own balance cache directly (in-process) instead of reflecting into a separate
 * plugin's private EconomyManager. Auction-house sale monitoring from the original tool isn't rebuilt
 * here - RealWorld doesn't have an auction house yet. */
public final class BalanceWatcher implements Runnable {

    private final EconomyManager economy;
    private final DiscordWebhook webhook;
    private final Supplier<Double> thresholdSupplier;
    private final Map<UUID, Double> lastSeen = new HashMap<>();

    public BalanceWatcher(EconomyManager economy, DiscordWebhook webhook, Supplier<Double> thresholdSupplier) {
        this.economy = economy;
        this.webhook = webhook;
        this.thresholdSupplier = thresholdSupplier;
    }

    @Override
    public void run() {
        double threshold = thresholdSupplier.get();
        for (Map.Entry<UUID, Double> entry : economy.allBalances().entrySet()) {
            Double previous = lastSeen.put(entry.getKey(), entry.getValue());
            if (previous == null) {
                continue;
            }
            double delta = entry.getValue() - previous;
            if (Math.abs(delta) >= threshold) {
                webhook.sendAlert(
                        delta > 0 ? "Large balance increase" : "Large balance decrease",
                        "Player " + entry.getKey() + "'s balance moved by " + String.format("%,.2f", delta)
                                + " (from " + String.format("%,.2f", previous) + " to " + String.format("%,.2f", entry.getValue()) + ").",
                        delta > 0 ? 0x57F287 : 0xED4245,
                        Map.of("Player UUID", entry.getKey().toString())
                );
            }
        }
    }
}
