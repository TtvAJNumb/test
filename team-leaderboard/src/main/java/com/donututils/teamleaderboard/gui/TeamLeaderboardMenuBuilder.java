package com.donututils.teamleaderboard.gui;

import com.donututils.teamleaderboard.model.StatKind;
import com.donututils.teamleaderboard.model.TeamStanding;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TeamLeaderboardMenuBuilder {

    private TeamLeaderboardMenuBuilder() {
    }

    public static List<ItemStack> build(List<TeamStanding> standings, StatKind kind) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < standings.size(); i++) {
            items.add(entryItem(standings.get(i), i + 1, kind));
        }
        return items;
    }

    private static ItemStack entryItem(TeamStanding standing, int rank, StatKind kind) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        var meta = item.getItemMeta();
        if (meta instanceof SkullMeta skullMeta && standing.leaderUuid() != null) {
            OfflinePlayer leader = Bukkit.getOfflinePlayer(standing.leaderUuid());
            skullMeta.setOwningPlayer(leader);
        }
        if (meta != null) {
            meta.setDisplayName(PagedMenu.legacy(rankColor(rank) + "#" + rank + " &f" + standing.teamName()));
            List<String> lore = new ArrayList<>();
            lore.add(PagedMenu.legacy("&7Members: &f" + standing.memberCount()));
            if (kind == StatKind.MONEY) {
                lore.add(PagedMenu.legacy("&7Combined money: &a$" + formatDouble(standing.totalMoney())));
            } else {
                lore.add(PagedMenu.legacy("&7Combined shards: &b" + formatLong(standing.totalShards())));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String rankColor(int rank) {
        return switch (rank) {
            case 1 -> "&6";
            case 2 -> "&7";
            case 3 -> "&c";
            default -> "&e";
        };
    }

    private static String formatDouble(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }

    private static String formatLong(long value) {
        return String.format(Locale.US, "%,d", value);
    }
}
