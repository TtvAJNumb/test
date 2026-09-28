package com.donututils.realworld.careers.onboarding;

import com.donututils.realworld.careers.citizen.CitizenManager;
import com.donututils.realworld.careers.config.CareersConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.function.Supplier;

public final class OnboardingListener implements Listener {

    private final Plugin plugin;
    private final CitizenManager citizenManager;
    private final OnboardingGuiService guiService;
    private final Supplier<CareersConfig> configSupplier;

    public OnboardingListener(Plugin plugin, CitizenManager citizenManager, OnboardingGuiService guiService,
                               Supplier<CareersConfig> configSupplier) {
        this.plugin = plugin;
        this.citizenManager = citizenManager;
        this.guiService = guiService;
        this.configSupplier = configSupplier;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (citizenManager.profileOf(player.getUniqueId()).hasChosenAgeTier()) {
            return;
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', configSupplier.get().welcomeMessage()));
        // Delayed a tick so the menu opens after the client has fully finished the join sequence.
        Bukkit.getScheduler().runTaskLater(plugin, () -> guiService.openAgeMenu(player), 20L);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof CareersMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        Runnable action = holder.actionFor(event.getRawSlot());
        if (action != null) {
            action.run();
        }
    }
}
