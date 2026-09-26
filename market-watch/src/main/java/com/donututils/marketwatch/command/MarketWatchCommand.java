package com.donututils.marketwatch.command;

import com.donututils.marketwatch.MarketWatchPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class MarketWatchCommand implements CommandExecutor {

    private final MarketWatchPlugin plugin;

    public MarketWatchCommand(MarketWatchPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /marketwatch <reload|status>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadMarketWatch();
                sender.sendMessage("MarketWatch config reloaded.");
            }
            case "status" -> sender.sendMessage("MarketWatch is running, bridged to UltimateDonutSmp. HTTP feed: "
                    + (plugin.isHttpEnabled() ? "enabled" : "disabled")
                    + ", Discord: " + (plugin.isWebhookConfigured() ? "configured" : "not configured"));
            default -> sender.sendMessage("Usage: /marketwatch <reload|status>");
        }
        return true;
    }
}
