package com.donututils.careers.onboarding;

import com.donututils.careers.citizen.CitizenManager;
import com.donututils.careers.config.AgeTier;
import com.donututils.careers.config.CareersConfig;
import com.donututils.careers.config.JobDefinition;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/** Builds and opens the two onboarding menus - both are plain chest-style inventories using
 * {@link CareersMenuHolder} so a single click listener can drive either screen. */
public final class OnboardingGuiService {

    private final CitizenManager citizenManager;
    private final Supplier<CareersConfig> configSupplier;

    public OnboardingGuiService(CitizenManager citizenManager, Supplier<CareersConfig> configSupplier) {
        this.citizenManager = citizenManager;
        this.configSupplier = configSupplier;
    }

    public void openAgeMenu(Player player) {
        CareersMenuHolder holder = new CareersMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 9, color("&6&lChoose Your Age"));
        holder.setInventory(inventory);

        inventory.setItem(3, icon(Material.EGG, "&bMinor", List.of("&7Entry-level jobs only.")));
        holder.onSlot(3, () -> {
            citizenManager.setAgeTier(player.getUniqueId(), AgeTier.MINOR);
            player.sendMessage(color("&aYou're now registered as a &bMinor&a."));
            player.closeInventory();
            openJobMenu(player);
        });

        inventory.setItem(5, icon(Material.IRON_CHESTPLATE, "&aAdult", List.of("&7Every job is open to you.")));
        holder.onSlot(5, () -> {
            citizenManager.setAgeTier(player.getUniqueId(), AgeTier.ADULT);
            player.sendMessage(color("&aYou're now registered as an &aAdult&a."));
            player.closeInventory();
            openJobMenu(player);
        });

        player.openInventory(inventory);
    }

    public void openJobMenu(Player player) {
        AgeTier tier = citizenManager.profileOf(player.getUniqueId()).ageTier();
        if (tier == null) {
            openAgeMenu(player);
            return;
        }

        Map<String, JobDefinition> jobs = configSupplier.get().jobsFor(tier);
        int size = Math.max(9, ((jobs.size() / 9) + 1) * 9);
        CareersMenuHolder holder = new CareersMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, size, color("&6&lChoose Your Job"));
        holder.setInventory(inventory);

        int slot = 0;
        for (JobDefinition job : jobs.values()) {
            List<String> lore = List.of(
                    "&7Wage: &f$" + String.format(Locale.US, "%,.2f", job.wageAmount()) + " &7every " + job.wageIntervalMinutes() + " min",
                    "&7Click to choose this job"
            );
            inventory.setItem(slot, icon(Material.PAPER, job.displayName(), lore));
            holder.onSlot(slot, () -> {
                citizenManager.setJob(player.getUniqueId(), job.id());
                player.sendMessage(color("&aYou're now working as a " + job.displayName() + "&a."));
                player.closeInventory();
            });
            slot++;
        }

        player.openInventory(inventory);
    }

    private static ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            meta.setLore(lore.stream().map(OnboardingGuiService::color).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
