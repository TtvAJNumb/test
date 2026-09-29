package com.donututils.realworld.ecowatch;

import com.donututils.realworld.RealWorldPlugin;
import com.donututils.realworld.ecowatch.discord.DiscordWebhook;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Locale;
import java.util.Map;

public final class EcoWatchCommand implements CommandExecutor {

    private final RealWorldPlugin plugin;
    private final DiscordWebhook webhook;

    public EcoWatchCommand(RealWorldPlugin plugin, DiscordWebhook webhook) {
        this.plugin = plugin;
        this.webhook = webhook;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&cUsage: /ecowatch <reload|test|status>"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadEcoWatch();
                sender.sendMessage(color("&aEconomyWatchdog reloaded."));
            }
            case "test" -> {
                webhook.sendAlert("Test alert", "This is a test alert from EconomyWatchdog, triggered by " + sender.getName() + ".",
                        0x5865F2, Map.of("Triggered by", sender.getName()));
                sender.sendMessage(color("&aTest alert sent."));
            }
            case "status" -> sender.sendMessage(color("&7Webhook configured: " + (webhook.isConfigured() ? "&ayes" : "&cno")));
            default -> sender.sendMessage(color("&cUsage: /ecowatch <reload|test|status>"));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
