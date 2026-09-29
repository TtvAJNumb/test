package com.donututils.donutrep.marketwatch;

import com.donututils.donutrep.DonutREPPlugin;
import com.donututils.donutrep.marketwatch.discord.DiscordWebhook;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Locale;

/**
 * /marketwatch reports total circulating Money via DonutREP's own EconomyManager, posted to Discord
 * on request.
 */
public final class MarketWatchCommand implements CommandExecutor {

    private final DonutREPPlugin plugin;
    private final DiscordWebhook webhook;

    public MarketWatchCommand(DonutREPPlugin plugin, DiscordWebhook webhook) {
        this.plugin = plugin;
        this.webhook = webhook;
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

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
