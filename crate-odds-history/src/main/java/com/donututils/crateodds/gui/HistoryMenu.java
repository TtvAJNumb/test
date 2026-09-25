package com.donututils.crateodds.gui;

import com.donututils.crateodds.model.HistoryEntry;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HistoryMenu {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter
            .ofPattern("MMM d, HH:mm", Locale.US)
            .withZone(ZoneId.systemDefault());

    private HistoryMenu() {
    }

    public static void open(Player player, String titleTemplate, String targetName, List<HistoryEntry> entries) {
        List<ItemStack> items = new ArrayList<>();
        // newest first
        List<HistoryEntry> sorted = new ArrayList<>(entries);
        sorted.sort((a, b) -> Long.compare(b.timestamp(), a.timestamp()));
        for (HistoryEntry entry : sorted) {
            items.add(buildItem(entry));
        }
        if (items.isEmpty()) {
            items.add(emptyItem());
        }
        CrateMenuHolder holder = new CrateMenuHolder(CrateMenuHolder.Type.HISTORY);
        String title = titleTemplate.replace("%player%", targetName);
        PagedMenu.open(player, holder, title, items);
    }

    private static ItemStack buildItem(HistoryEntry entry) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(PagedMenu.legacy("&6" + entry.crateId()));

            List<Component> lore = new ArrayList<>();
            lore.add(PagedMenu.legacy("&eGot: &f" + entry.rewardDisplayName()));
            lore.add(PagedMenu.legacy("&eDetail: &f" + entry.grantSummary()));
            lore.add(PagedMenu.legacy(String.format(Locale.US, "&eOdds at the time: &f%.2f%%", entry.percentAtTime())));
            lore.add(PagedMenu.legacy("&7" + FORMAT.format(Instant.ofEpochMilli(entry.timestamp()))));
            meta.lore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack emptyItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(PagedMenu.legacy("&cNo crate openings recorded yet"));
            item.setItemMeta(meta);
        }
        return item;
    }
}
