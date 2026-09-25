package com.donututils.ecowatch.watch;

import com.donututils.ecowatch.config.EcoWatchConfig;
import com.donututils.ecowatch.discord.DiscordWebhook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;
import java.util.Map;

/**
 * Watches for large {@code /pay <player> <amount>} commands the same way CrateBindAddon's own
 * BindListener watches for {@code /create binditem} - a MONITOR-priority, non-cancelling
 * PlayerCommandPreprocessEvent listener, since UDS fires no event of its own for a transfer.
 * <p>
 * This fires on the amount as <em>typed</em>, not on a confirmed transfer - UDS doesn't expose
 * whether the pay actually succeeded (e.g. insufficient funds), so an attempted large pay that
 * failed will still raise an alert. Treat alerts as "worth a look", not proof of a completed
 * transfer.
 */
public final class PayCommandWatcher implements Listener {

    private final DiscordWebhook webhook;
    private final EcoWatchConfig config;

    public PayCommandWatcher(DiscordWebhook webhook, EcoWatchConfig config) {
        this.webhook = webhook;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!config.largeTransferEnabled()) {
            return;
        }

        String[] parts = event.getMessage().trim().split("\\s+");
        if (parts.length < 3) {
            return;
        }

        String label = parts[0].startsWith("/") ? parts[0].substring(1) : parts[0];
        int colon = label.indexOf(':');
        if (colon >= 0) {
            label = label.substring(colon + 1);
        }
        if (!label.equalsIgnoreCase("pay")) {
            return;
        }

        String target = parts[1];
        double amount;
        try {
            amount = Double.parseDouble(parts[2]);
        } catch (NumberFormatException ex) {
            return;
        }
        if (amount < config.minTransferAmount()) {
            return;
        }

        Player sender = event.getPlayer();
        webhook.sendAlert(
                "Large /pay transfer",
                sender.getName() + " attempted to pay " + target + " $" + format(amount) + ".",
                0xFEE75C,
                Map.of("From", sender.getName(), "To", target, "Amount", "$" + format(amount))
        );
    }

    private static String format(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }
}
