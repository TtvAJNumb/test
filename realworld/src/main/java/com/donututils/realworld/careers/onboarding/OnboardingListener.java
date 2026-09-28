package com.donututils.realworld.careers.onboarding;

import com.donututils.realworld.careers.citizen.CitizenManager;
import com.donututils.realworld.careers.config.CareersConfig;
import com.donututils.realworld.careers.model.CitizenProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
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

    /** Picking an age/job is mandatory: if a player dismisses one of these menus (Escape, hotbar
     * swap, etc.) without actually choosing anything, put the right one straight back in front of
     * them a moment later instead of leaving them stuck with no age tier or no job. Skipped when the
     * close is the plugin's own age-menu-to-job-menu handoff, not the player walking away - see
     * {@link OnboardingGuiService#consumeInternalTransition}. */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof CareersMenuHolder)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (guiService.consumeInternalTransition(player.getUniqueId())) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            CitizenProfile profile = citizenManager.profileOf(player.getUniqueId());
            if (!profile.hasChosenAgeTier()) {
                guiService.openAgeMenu(player);
            } else if (!profile.hasChosenJob()) {
                guiService.openJobMenu(player);
            }
        }, 2L);
    }
}
