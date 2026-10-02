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
        t.put("balance", new String[]{
                "&6&lBalance &7- /balance [player] (aliases: /bal, /money)",
                "&7Check your (or another player's) Money balance."
        });
        t.put("shop", new String[]{
                "&6&lShop &7- /shop",
                "&7Open the shop - End, Nether, Gear, Food, Shard, and Crate Keys categories, matching real UltimateDonutSmp."
        });
        t.put("shopadmin", new String[]{
                "&6&lShopAdmin &7- /shopadmin reload",
                "&7Reload the shop's config.yml entries."
        });
        t.put("sell", new String[]{
                "&6&lSell &7- /sell, /sellhand, /sellall, /sellmulti <material> <amount>, /sellmultiplier [value], /sellprogress [player], /sellhistory, /topsell, /worth [material]",
                "&7Sell items for Money at real UltimateDonutSmp worth.yml prices - a separate system from /shop's buy catalog."
        });
        t.put("worth", t.get("sell"));
        t.put("auctionhouse", new String[]{
                "&6&lAuction House &7- /auctionhouse <sell <price>|list [page]|buy <id>|my|claims|cancel <id>|reload> (alias: /ah)",
                "&7The player-to-player marketplace - list your items or buy others'."
        });
        t.put("ah", t.get("auctionhouse"));
        t.put("orders", new String[]{
                "&6&lOrders &7- /orders [create <price> <amount>|fulfill <id>|my|collect|cancel <id>]",
                "&7Post a buy request for an item (hold a sample + set price/amount); anyone can fulfill it for the payout."
        });
        t.put("shopedit", new String[]{
                "&6&lShopEdit &7- /shopedit remove <id>",
                "&7Staff moderation - force-remove any Auction House listing."
        });
        t.put("chat", new String[]{
                "&6&lChat &7- /chat <help|mute|unmute|delay <seconds>|clear>",
                "&7Staff command - administer global chat (mute-all, slow-mode delay, or clear the screen)."
        });
        t.put("msg", new String[]{
                "&6&lMsg &7- /msg <player> <message> (alias: /pm)",
                "&7Send a private message. /reply <message> answers your last conversation."
        });
        t.put("pm", t.get("msg"));
        t.put("reply", new String[]{
                "&6&lReply &7- /reply <message>",
                "&7Answer whoever last messaged you (or you last messaged)."
        });
        t.put("ignore", new String[]{
                "&6&lIgnore &7- /ignore <player>, /unignore <player>",
                "&7Stop (or resume) receiving a player's private messages and chat lines."
        });
        t.put("home", new String[]{
                "&6&lHome &7- /home [name], /homes, /sethome [name], /delhome <name>, /renamehome <old> <new>",
                "&7Set and teleport to named homes."
        });
        t.put("homes", t.get("home"));
        t.put("spawn", new String[]{
                "&6&lSpawn &7- /spawn (players), /setspawn (admin)",
                "&7Teleport to the server's spawn point, or set it to your current location."
        });
        t.put("afk", new String[]{
                "&6&lAFK &7- /afk (toggle your own), /setafk <player> [on|off] (staff)",
                "&7Mark yourself (or, for staff, another player) as away from keyboard."
        });
        t.put("rtp", new String[]{
                "&6&lRTP &7- /rtp, /rtpq",
                "&7Random-teleport to a nearby location. /rtpq queues you when too many are running."
        });
        t.put("warp", new String[]{
                "&6&lWarp &7- /warp [name], /setwarp <name>, /delwarp <name>, /warpmanager, /portalmanager",
                "&7Teleport to named warps - staff can also bind physical portal regions to them."
        });
        t.put("tpa", new String[]{
                "&6&lTPA &7- /tpa <player>, /tpahere <player>, /tpaccept, /tpadeny, /tpacancel, /tpauto, /tpahereauto",
                "&7Send or answer a player-to-player teleport request."
        });
        t.put("duel", new String[]{
                "&6&lDuel &7- /duel <player|accept|decline>, /queue, /leave",
                "&7Challenge a player to a 1v1, or /queue for a random opponent. /leave forfeits."
        });
        t.put("queue", t.get("duel"));
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
        t.put("staff", new String[]{
                "&6&lStaff Tools &7- /freeze /fly /flyspeed /heal /feed /gamemode /god /vanish /invsee /staffmode /stafflist /staffchat /helpop /report /rename",
                "&7Standalone moderator tools - freeze a player, fly, heal/feed, change game mode, god mode, vanish, inspect an inventory, stash your gear for investigating, staff chat, helpop, reports, and item renaming."
        });
        t.put("freeze", t.get("staff"));
        t.put("fly", t.get("staff"));
        t.put("gamemode", t.get("staff"));
        t.put("vanish", t.get("staff"));
        t.put("invsee", t.get("staff"));
        t.put("staffmode", t.get("staff"));
        t.put("staffchat", t.get("staff"));
        t.put("ahstats", new String[]{
                "&6&lAhStats &7- /ahstats",
                "&7Shows real Auction House activity - the highest-priced active listings."
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
