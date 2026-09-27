package com.donututils.stockmarket.listener;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongConsumer;

/**
 * A one-shot "type a number in chat" prompt, used for Buy/Sell quantity instead of placing a real
 * sign or block in the world - simpler and leaves the player's build untouched. Uses the classic
 * {@link AsyncPlayerChatEvent} rather than a Paper-only chat event, since this plugin is meant to
 * run on any Bukkit/Spigot/Paper server, not just Paper.
 */
public final class ChatQuantityPrompt implements Listener {

    private final Plugin plugin;
    private final Map<UUID, LongConsumer> pending = new ConcurrentHashMap<>();

    public ChatQuantityPrompt(Plugin plugin) {
        this.plugin = plugin;
    }

    public void prompt(Player player, String message, LongConsumer onQuantity) {
        player.sendMessage(legacy(message));
        player.sendMessage(legacy("&7Type a whole number in chat, or type &fcancel&7 to back out."));
        pending.put(player.getUniqueId(), onQuantity);
    }

    public void cancel(UUID playerId) {
        pending.remove(playerId);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        LongConsumer callback = pending.get(event.getPlayer().getUniqueId());
        if (callback == null) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage().trim();
        Player player = event.getPlayer();

        if (message.equalsIgnoreCase("cancel")) {
            pending.remove(player.getUniqueId());
            plugin.getServer().getScheduler().runTask(plugin, () -> player.sendMessage(legacy("&7Cancelled.")));
            return;
        }

        long quantity;
        try {
            quantity = Long.parseLong(message);
        } catch (NumberFormatException ex) {
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    player.sendMessage(legacy("&cThat's not a whole number - try again, or type cancel.")));
            return;
        }

        pending.remove(player.getUniqueId());
        // AsyncPlayerChatEvent fires off the main thread on most servers - hop back before touching
        // economy/Bukkit state.
        plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(quantity));
    }

    private static String legacy(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
