package com.donututils.realworld.careers.onboarding;

import com.donututils.realworld.careers.citizen.CitizenManager;
import com.donututils.realworld.careers.config.AgeTier;
import com.donututils.realworld.careers.config.CareersConfig;
import com.donututils.realworld.careers.config.JobDefinition;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

        openInventorySafely(player, inventory);
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

        openInventorySafely(player, inventory);
    }

    /** Bukkit/Paper's HumanEntity#openInventory has returned different types (void on some very old
     * builds, InventoryView on modern ones) across API versions - compiling a direct call against the
     * wrong one produces an AbstractMethodError at runtime that silently breaks every click, since the
     * menu either never opens or the interface call site doesn't resolve. Called reflectively instead,
     * the same defensive pattern already used by every other GUI subsystem in this plugin
     * (see StockMarket's PagedMenu). This was the actual cause of "setting up age and choosing a
     * career don't work" - the menus were failing to open at all on the real server. */
    private static void openInventorySafely(Player player, Inventory inventory) {
        try {
            Method method = player.getClass().getMethod("openInventory", Inventory.class);
            method.invoke(player, inventory);
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            throw new IllegalStateException("Could not find a way to open an inventory on this server: " + ex, ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new IllegalStateException("Opening the menu inventory failed: " + cause, cause);
        }
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
