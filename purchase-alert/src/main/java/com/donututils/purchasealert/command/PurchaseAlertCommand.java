package com.donututils.purchasealert.command;

import com.donututils.purchasealert.PurchaseAlertPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Locale;
import java.util.Map;

public final class PurchaseAlertCommand implements CommandExecutor {

    private final PurchaseAlertPlugin plugin;

    public PurchaseAlertCommand(PurchaseAlertPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(legacy("&cUsage: /purchasealert <reload|test|status>"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadWatchdog();
                sender.sendMessage(legacy("&aPurchaseAlert reloaded."));
            }
            case "test" -> {
                plugin.getWebhook().sendAlert(
                        "Test alert",
                        "This is a test alert from PurchaseAlert, triggered by " + sender.getName() + ".",
                        0x5865F2,
                        Map.of("Triggered by", sender.getName())
                );
                sender.sendMessage(legacy("&aTest alert sent - check your Discord channel."));
            }
            case "status" -> {
                boolean configured = plugin.getWebhook().isConfigured();
                sender.sendMessage(legacy("&7Webhook configured: " + (configured ? "&ayes" : "&cno (set discord.webhook-url in config.yml)")));
            }
            default -> sender.sendMessage(legacy("&cUsage: /purchasealert <reload|test|status>"));
        }
        return true;
    }

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
