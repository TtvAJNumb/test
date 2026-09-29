package com.donututils.donutrep.help;

import java.util.LinkedHashMap;
import java.util.Map;

/** Every /help topic's lines, keyed by the id players type after /help. Kept as one static registry
 * rather than scattering "usage" strings across every subsystem, so /help stays a single source of
 * truth admins can scan top to bottom. */
public final class HelpTopics {

    private HelpTopics() {
    }

    public static final Map<String, String[]> TOPICS = build();

    private static Map<String, String[]> build() {
        Map<String, String[]> t = new LinkedHashMap<>();

        t.put("pay", new String[]{
                "&6&lPay &7- /pay <player> <amount>",
                "&7Send Money to another online player."
        });
        t.put("shards", new String[]{
                "&6&lShards &7- /shards [player]",
                "&7Check your (or another player's) Shards balance - the premium currency."
        });
        t.put("shop", new String[]{
                "&6&lShop &7- /shop",
                "&7Open the shop - End, Nether, Gear, Food, Shard, and Crate Keys categories, matching real UltimateDonutSmp."
        });
        t.put("shopadmin", new String[]{
                "&6&lShopAdmin &7- /shopadmin reload",
                "&7Reload the shop's config.yml entries."
        });
        t.put("crate", new String[]{
                "&6&lCrate &7- /crate <bind <id>|unbind|set <player> <crate> <amount>|list|reload>",
                "&7Bind a chest/barrel/ender chest/shulker box to a crate, or give crate keys."
        });
        t.put("cratebind", t.get("crate"));
        t.put("team", new String[]{
                "&6&lTeam &7- /team <create <name>|disband|add <player>|remove <player>|leave|info [player]|list>",
                "&7Form a team with friends - /teambaltop and /teamshardstop rank teams by it."
        });
        t.put("sus", new String[]{
                "&6&lSus &7- /sus [player|reload]",
                "&7Staff panel: browse online players, freeze/unfreeze, view inventory/ender chest."
        });
        t.put("punishhistory", new String[]{
                "&6&lPunishHistory &7- /punishhistory [player]",
                "&7View a player's staff notes."
        });
        t.put("note", new String[]{
                "&6&lNote &7- /note <add|remove|list> <player> [text|index]",
                "&7Manage staff notes on a player."
        });
        t.put("ecowatch", new String[]{
                "&6&lEcoWatch &7- /ecowatch <reload|test|status>",
                "&7Manage EconomyWatchdog's Discord balance-jump alerts."
        });
        t.put("marketwatch", new String[]{
                "&6&lMarketWatch &7- /marketwatch <reload|status>",
                "&7Check total circulating Money and MarketWatch's webhook status."
        });
        t.put("purchasealert", new String[]{
                "&6&lPurchaseAlert &7- /purchasealert <reload|test|status>",
                "&7Manage Discord alerts for new store purchases (reads StoreBridge's backend)."
        });
        t.put("teamleaderboard", new String[]{
                "&6&lTeamLeaderboard &7- /teambaltop, /teamshardstop",
                "&7Ranks teams by combined member Money or Shards."
        });
        t.put("addshards", new String[]{
                "&6&lAddShards &7- /addshards <player> <amount>",
                "&7Console command StoreBridge runs to deliver a Shards store package."
        });
        t.put("removeshards", new String[]{
                "&6&lRemoveShards &7- /removeshards <player> <amount>",
                "&7Console command StoreBridge runs on a chargeback to claw back Shards."
        });

        return t;
    }
}
