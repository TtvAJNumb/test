package com.donututils.careers.job;

import com.donututils.careers.citizen.CitizenManager;
import com.donututils.careers.config.CareersConfig;
import com.donututils.careers.config.JobDefinition;
import com.donututils.careers.economy.VaultEconomyBridge;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.Map;
import java.util.function.Supplier;

/** Pays real task bonuses straight from vanilla events - no dependency on any other plugin. Only
 * jobs whose {@code block-bonuses}/{@code fish-catch-bonus}/{@code breed-bonus} are configured earn
 * anything here; every other job is passive-wage-only (see config.yml comments). */
public final class TaskBonusListener implements Listener {

    private final CitizenManager citizenManager;
    private final Supplier<CareersConfig> configSupplier;
    private final VaultEconomyBridge economy;

    public TaskBonusListener(CitizenManager citizenManager, Supplier<CareersConfig> configSupplier, VaultEconomyBridge economy) {
        this.citizenManager = citizenManager;
        this.configSupplier = configSupplier;
        this.economy = economy;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String jobId = citizenManager.profileOf(player.getUniqueId()).jobId();
        if (jobId == null) {
            return;
        }
        JobDefinition job = configSupplier.get().job(jobId);
        if (job == null || job.blockBonuses().isEmpty()) {
            return;
        }
        Double bonus = job.blockBonuses().get(event.getBlock().getType().name());
        if (bonus != null) {
            citizenManager.payBonusIfEligible(player, jobId, bonus, job.displayName() + " task bonus", economy);
        }
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Player player = event.getPlayer();
        String jobId = citizenManager.profileOf(player.getUniqueId()).jobId();
        if (jobId == null) {
            return;
        }
        JobDefinition job = configSupplier.get().job(jobId);
        if (job == null || job.fishCatchBonus() <= 0) {
            return;
        }
        citizenManager.payBonusIfEligible(player, jobId, job.fishCatchBonus(), job.displayName() + " catch bonus", economy);
    }

    @EventHandler
    public void onBreed(EntityBreedEvent event) {
        LivingEntity breeder = event.getBreeder();
        if (!(breeder instanceof Player player)) {
            return;
        }
        String jobId = citizenManager.profileOf(player.getUniqueId()).jobId();
        if (jobId == null) {
            return;
        }
        JobDefinition job = configSupplier.get().job(jobId);
        if (job == null || job.breedBonus() <= 0) {
            return;
        }
        citizenManager.payBonusIfEligible(player, jobId, job.breedBonus(), job.displayName() + " breeding bonus", economy);
    }
}
