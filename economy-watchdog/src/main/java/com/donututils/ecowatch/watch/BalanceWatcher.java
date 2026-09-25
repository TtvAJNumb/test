package com.donututils.ecowatch.watch;

import com.donututils.ecowatch.config.EcoWatchConfig;
import com.donututils.ecowatch.discord.DiscordWebhook;
import com.donututils.ecowatch.reflect.UdsBridge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Polls every online player's balance each cycle and flags jumps against their balance on the
 * previous poll. UDS fires no event for deposits/withdrawals/transfers, so this is the only
 * server-side signal available without patching UDS itself; it only sees online players, and
 * only balance changes that happen between two polls (a rapid deposit-then-withdraw within one
 * interval would net out and go unnoticed - shorten poll-interval-seconds to tighten that).
 */
public final class BalanceWatcher implements Runnable {

    private final Plugin plugin;
    private final UdsBridge bridge;
    private final DiscordWebhook webhook;
    private final EcoWatchConfig config;

    private final Map<UUID, Double> lastBalances = new HashMap<>();

    public BalanceWatcher(Plugin plugin, UdsBridge bridge, DiscordWebhook webhook, EcoWatchConfig config) {
        this.plugin = plugin;
        this.bridge = bridge;
        this.webhook = webhook;
        this.config = config;
    }

    @Override
    public void run() {
        if (!config.balanceJumpEnabled()) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            double current;
            try {
                current = bridge.getBalance(player.getUniqueId());
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Failed to read balance for " + player.getName() + ": " + ex.getMessage());
                continue;
            }

            Double previous = lastBalances.put(player.getUniqueId(), current);
            if (previous == null) {
                continue; // first sighting this session - nothing to compare against yet
            }

            double delta = current - previous;
            double absDelta = Math.abs(delta);
            boolean overAbsolute = absDelta >= config.minAbsoluteDelta();
            boolean overPercent = previous > 0 && (absDelta / previous) * 100.0 >= config.minPercentDelta();

            if (overAbsolute || overPercent) {
                alert(player, previous, current, delta);
            }
        }
    }

    private void alert(Player player, double previous, double current, double delta) {
        String direction = delta >= 0 ? "up" : "down";
        int color = delta >= 0 ? 0x57F287 : 0xED4245;
        webhook.sendAlert(
                "Suspicious balance jump",
                player.getName() + "'s balance moved " + direction + " by $" + format(Math.abs(delta))
                        + " within the last " + config.pollIntervalSeconds() + "s.",
                color,
                Map.of(
                        "Player", player.getName(),
                        "Previous", "$" + format(previous),
                        "Current", "$" + format(current),
                        "Delta", (delta >= 0 ? "+$" : "-$") + format(Math.abs(delta))
                )
        );
    }

    private static String format(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }
}
