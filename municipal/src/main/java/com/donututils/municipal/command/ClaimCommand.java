package com.donututils.municipal.command;

import com.donututils.municipal.MunicipalPlugin;
import com.donututils.municipal.claim.ClaimManager;
import com.donututils.municipal.economy.VaultEconomyBridge;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class ClaimCommand implements CommandExecutor {

    private final MunicipalPlugin plugin;
    private final ClaimManager claimManager;
    private final VaultEconomyBridge economy;

    public ClaimCommand(MunicipalPlugin plugin, ClaimManager claimManager, VaultEconomyBridge economy) {
        this.plugin = plugin;
        this.claimManager = claimManager;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&cUsage: /municipal [claim|unclaim|map|reload]"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "claim" -> claim(sender);
            case "unclaim" -> unclaim(sender);
            case "map" -> map(sender);
            case "reload" -> reload(sender);
            default -> sender.sendMessage(color("&cUsage: /municipal [claim|unclaim|map|reload]"));
        }
        return true;
    }

    private void claim(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        // ClaimManager#claim blocks on a database insert to get a definite result back - never call
        // it directly from a command handler on the main thread.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ClaimManager.ClaimResult result = claimManager.claim(player, economy);
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(color((result.success() ? "&a" : "&c") + result.message())));
        });
    }

    private void unclaim(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ClaimManager.ClaimResult result = claimManager.unclaim(player);
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(color((result.success() ? "&a" : "&c") + result.message())));
        });
    }

    private void map(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        List<String> rows = claimManager.renderMap(player.getLocation(), player, 4);
        player.sendMessage(color("&6&lNearby Claims &7(&a#&7=you &c#&7=other &7[ ]=unclaimed &e[X]&7=you are here)"));
        for (String row : rows) {
            player.sendMessage(color(row));
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("municipal.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return;
        }
        plugin.reloadMunicipal();
        sender.sendMessage(color("&aMunicipal config reloaded."));
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(color("&cOnly players can do that."));
        return null;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
