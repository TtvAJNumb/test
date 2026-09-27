package com.donututils.municipal.jail;

import com.donututils.municipal.config.MunicipalConfig;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Locale;
import java.util.function.Supplier;

/** Keeps a jailed player at the jail point and blocks everything but a small command whitelist. */
public final class JailListener implements Listener {

    private final JailManager jailManager;
    private final Supplier<MunicipalConfig> configSupplier;

    public JailListener(JailManager jailManager, Supplier<MunicipalConfig> configSupplier) {
        this.jailManager = jailManager;
        this.configSupplier = configSupplier;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        jailManager.onRejoin(event.getPlayer());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (jailManager.isJailed(player.getUniqueId())) {
            jailManager.enforceRadius(player);
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!jailManager.isJailed(player.getUniqueId())) {
            return;
        }
        String message = event.getMessage();
        String label = message.substring(1).split(" ", 2)[0].toLowerCase(Locale.ROOT);
        if (configSupplier.get().isCommandAllowedWhileJailed(label)) {
            return;
        }
        event.setCancelled(true);
        player.sendMessage(legacy("&cYou can't use that command while jailed."));
    }

    private static String legacy(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
