package com.donututils.crateodds.gui;

import com.donututils.crateodds.model.CrateSnapshot;
import com.donututils.crateodds.model.RewardOdds;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class OddsMenu {

    private OddsMenu() {
    }

    public static void open(Player player, String titleTemplate, CrateSnapshot snapshot) {
        List<ItemStack> items = new ArrayList<>();
        for (RewardOdds reward : snapshot.rewards()) {
            items.add(buildItem(reward));
        }
        if (items.isEmpty()) {
            items.add(emptyItem());
        }
        CrateMenuHolder holder = new CrateMenuHolder(CrateMenuHolder.Type.ODDS);
        String title = titleTemplate.replace("%crate%", snapshot.crateId());
        PagedMenu.open(player, holder, title, items);
    }

    private static ItemStack buildItem(RewardOdds reward) {
        int amount = Math.max(1, Math.min(64, reward.amount()));
        ItemStack item = new ItemStack(reward.icon(), amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(PagedMenu.legacy("&f" + reward.displayName()));

            List<Component> lore = new ArrayList<>();
            for (String line : reward.lore()) {
                lore.add(PagedMenu.legacy("&7" + line));
            }
            if (!lore.isEmpty()) {
                lore.add(PagedMenu.legacy(" "));
            }
            lore.add(PagedMenu.legacy("&eGrants: &f" + reward.grantSummary()));
            lore.add(PagedMenu.legacy(String.format(Locale.US, "&eChance: &f%.2f%%", reward.percent())));
            lore.add(PagedMenu.legacy("&7(weight " + reward.weight() + ")"));
            meta.lore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack emptyItem() {
        ItemStack item = new ItemStack(org.bukkit.Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(PagedMenu.legacy("&cThis crate has no rewards configured"));
            item.setItemMeta(meta);
        }
        return item;
    }
}
