package com.donututils.realworld.help;

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

        t.put("bank", new String[]{
                "&6&lBank &7- /bank [deposit|withdraw|balance|credit]",
                "&7Move Money between checking and savings, check your balance and credit score."
        });
        t.put("pay", new String[]{
                "&6&lPay &7- /pay <player> <amount>",
                "&7Send Money to another online player."
        });
        t.put("loan", new String[]{
                "&6&lLoan &7- /loan [request|repay|status]",
                "&7Borrow Money against your credit score, or repay/check an existing loan."
        });
        t.put("corp", new String[]{
                "&6&lCorp &7- /corp [found|report|info|dividend|trust]",
                "&7Found a corporation, report financials, or pay dividends to shareholders."
        });
        t.put("stock", new String[]{
                "&6&lStock &7- /stock [buy|sell|list|portfolio]",
                "&7Trade shares of a corporation founded with /corp."
        });
        t.put("stocks", new String[]{
                "&6&lStocks &7- /stocks [price|buy|sell]",
                "&7Open the public stock market GUI, or buy/sell/check a price from chat."
        });
        t.put("shop", new String[]{
                "&6&lShop &7- /shop [sell hand]",
                "&7Open the categorized item market (Overworld/Nether/End/Firearms/Motors/Permits)."
        });
        t.put("shards", new String[]{
                "&6&lShards &7- /shards [player]",
                "&7Check your (or another player's) Shards balance - the premium currency."
        });
        t.put("crate", new String[]{
                "&6&lCrate &7- /crate <bind <id>|unbind|set <player> <crate> <amount>|list|reload>",
                "&7Bind a chest/barrel/ender chest/shulker box to a crate, or give crate keys."
        });
        t.put("cratebind", t.get("crate"));
        t.put("career", new String[]{
                "&6&lCareer &7- /career [info|list|choose <job>|reopen|legacy|reload]",
                "&7Check your age/job, pick a job, or reset your life as a legacy for a wage bonus."
        });
        t.put("permit", new String[]{
                "&6&lPermit &7- /permit [buy <business|building|weapon>|check [player]]",
                "&7Buy or check business/building/weapon permits."
        });
        t.put("municipal", new String[]{
                "&6&lMunicipal &7- /municipal [claim|unclaim|map|reload]",
                "&7Claim/unclaim the chunk you're standing in and view nearby claims."
        });
        t.put("location", new String[]{
                "&6&lLocation &7- /location [pos1|pos2|save <name>|remove <name>|list|info <name>|tp <name>]",
                "&7Define named warps or cuboid regions (jail, market, etc) and teleport to them."
        });
        t.put("police", new String[]{
                "&6&lPolice &7- /police arrest <player> <reason>",
                "&7Arrest a player, opening a pending court case."
        });
        t.put("court", new String[]{
                "&6&lCourt &7- /court [sentence <caseId> <minutes> <reason>|dismiss <caseId> [reason]|release <player>|record <player>]",
                "&7Preside over trials - sentence, dismiss, release, or check a criminal record."
        });
        t.put("order", t.get("court"));
        t.put("arsenal", new String[]{
                "&6&lArsenal &7- /arsenal [give <player> <weapon>|giveammo <player> <weapon> [amount]|list|reload]",
                "&7Admin command to give weapons/ammo directly - players buy them via /shop instead."
        });
        t.put("motors", new String[]{
                "&6&lMotors &7- /motors [give <player> <vehicle>|list|info|refuel <amount>|repair|reload]",
                "&7Give vehicle spawners, or check/refuel/repair the vehicle you're driving."
        });
        t.put("team", new String[]{
                "&6&lTeam &7- /team <create <name>|disband|add <player>|remove <player>|leave|info [player]|list>",
                "&7Form a team with friends - /teambaltop and /teamshardstop rank teams by it."
        });
        t.put("sus", new String[]{
                "&6&lSus &7- /sus [player|reload]",
                "&7Staff panel: browse online players, freeze/unfreeze, view inventory/ender chest."
        });
        t.put("ah", new String[]{
                "&6&lAhStats &7- /ahstats [symbol]",
                "&7No auction house exists yet - this shows StockMarket price history instead."
        });
        t.put("ahstats", t.get("ah"));
        t.put("punishhistory", new String[]{
                "&6&lPunishHistory &7- /punishhistory [player]",
                "&7View a player's court record (arrests/convictions/dismissals) and staff notes."
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
        t.put("ledgeradmin", new String[]{
                "&6&lLedgerAdmin &7- /ledgeradmin <reload|forgiveloan>",
                "&7Administer the Ledger economy."
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
