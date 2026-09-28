package com.donututils.realworld.stockmarket.listener;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

/**
 * A one-shot "type something in chat" prompt, used for Buy/Sell quantity and for confirming a trade
 * preview, instead of placing a real sign or block in the world - simpler and leaves the player's
 * build untouched. Uses the classic {@link AsyncPlayerChatEvent} rather than a Paper-only chat
 * event, since this plugin is meant to run on any Bukkit/Spigot/Paper server, not just Paper.
 */
public final class ChatQuantityPrompt implements Listener {

    private final Plugin plugin;
    private final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();

    public ChatQuantityPrompt(Plugin plugin) {
        this.plugin = plugin;
    }

    public void prompt(Player player, String message, LongConsumer onQuantity) {
        player.sendMessage(legacy(message));
        registerQuantityHandler(player, onQuantity);
    }

    private void registerQuantityHandler(Player player, LongConsumer onQuantity) {
        player.sendMessage(legacy("&7Type a whole number in chat, or type &fcancel&7 to back out."));
        pending.put(player.getUniqueId(), raw -> {
            long quantity;
            try {
                quantity = Long.parseLong(raw);
            } catch (NumberFormatException ex) {
                player.sendMessage(legacy("&cThat's not a whole number - try again, or type cancel."));
                registerQuantityHandler(player, onQuantity);
                return;
            }
            onQuantity.accept(quantity);
        });
    }

    /** Waits for "confirm" (runs onConfirm) or "cancel"/anything else (just cancels). */
    public void promptConfirmation(Player player, String message, Runnable onConfirm) {
        player.sendMessage(legacy(message));
        player.sendMessage(legacy("&7Type &fconfirm&7 to proceed, or &fcancel&7 to back out."));
        pending.put(player.getUniqueId(), raw -> {
            if (raw.equalsIgnoreCase("confirm")) {
                onConfirm.run();
            } else {
                player.sendMessage(legacy("&7Cancelled."));
            }
        });
    }

    public void cancel(UUID playerId) {
        pending.remove(playerId);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Consumer<String> callback = pending.get(event.getPlayer().getUniqueId());
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

        pending.remove(player.getUniqueId());
        // AsyncPlayerChatEvent fires off the main thread on most servers - hop back before touching
        // economy/Bukkit state.
        plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(message));
    }

    private static String legacy(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
