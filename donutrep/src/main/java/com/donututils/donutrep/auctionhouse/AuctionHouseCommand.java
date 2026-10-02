package com.donututils.donutrep.auctionhouse;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

/** /auctionhouse (alias /ah) - the real player-to-player marketplace, matching real UDS's command
 * name and subcommands (sell|my|claims|cancel|reload). "list" and "buy" are necessary additions
 * beyond that literal usage string - real UDS opens a GUI to browse/buy where this is chat-based
 * instead. There's no separate claims inbox: proceeds from a sale are deposited to your Money
 * balance immediately, so /auctionhouse claims just confirms that rather than handing over a queued
 * payout. */
public final class AuctionHouseCommand implements CommandExecutor {

    private static final int PAGE_SIZE = 8;

    private final AuctionHouseManager auctionHouse;

    public AuctionHouseCommand(AuctionHouseManager auctionHouse) {
        this.auctionHouse = auctionHouse;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        if (args.length < 1) {
            list(player, args);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "sell" -> sell(player, args);
            case "list" -> list(player, args);
            case "buy" -> buy(player, args);
            case "my", "mine" -> mine(player);
            case "cancel" -> cancel(player, args);
            case "claims" -> player.sendMessage(color("&7Sale proceeds are deposited to your Money balance automatically - nothing pending to claim."));
            case "reload" -> player.sendMessage(color("&aAuction House has no reloadable config."));
            default -> sendUsage(player);
        }
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(color("&cUsage: /auctionhouse <sell <price>|list [page]|buy <id>|my|claims|cancel <id>|reload>"));
    }

    private void sell(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /auctionhouse sell <price>"));
            return;
        }
        double price;
        try {
            price = Double.parseDouble(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(color("&cInvalid price."));
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            player.sendMessage(color("&cHold the item you want to sell first."));
            return;
        }
        ItemStack toList = hand.clone();
        AuctionHouseManager.ListResult result = auctionHouse.list(player, toList, price);
        switch (result) {
            case SUCCESS -> {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                player.sendMessage(color("&aListed " + toList.getAmount() + "x " + displayName(toList) + " for " + formatMoney(price) + "."));
            }
            case TOO_MANY_LISTINGS -> player.sendMessage(color("&cYou already have too many active listings - cancel one first."));
            case INVALID_PRICE -> player.sendMessage(color("&cPrice must be positive."));
        }
    }

    private void list(Player player, String[] args) {
        List<AuctionListing> listings = auctionHouse.activeListings();
        if (listings.isEmpty()) {
            player.sendMessage(color("&7The auction house is empty."));
            return;
        }
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException ignored) {
                // stay on page 1
            }
        }
        int totalPages = (listings.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        page = Math.min(page, totalPages);
        int from = (page - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, listings.size());

        player.sendMessage(color("&6&lAuction House &7(page " + page + "/" + totalPages + ")"));
        for (AuctionListing listing : listings.subList(from, to)) {
            player.sendMessage(color("&e#" + listing.id() + " &f" + listing.amount() + "x " + displayName(listing)
                    + " &7- " + formatMoney(listing.price()) + " &7(by " + listing.sellerName() + ") &8/auctionhouse buy " + listing.id()));
        }
    }

    private void buy(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /auctionhouse buy <id>"));
            return;
        }
        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(color("&cInvalid listing id."));
            return;
        }
        AuctionHouseManager.BuyResult result = auctionHouse.buy(player, id);
        switch (result) {
            case SUCCESS -> player.sendMessage(color("&aPurchased listing #" + id + "."));
            case NOT_FOUND -> player.sendMessage(color("&cNo listing #" + id + "."));
            case INSUFFICIENT_FUNDS -> player.sendMessage(color("&cYou can't afford that."));
            case OWN_LISTING -> player.sendMessage(color("&cYou can't buy your own listing - use /auctionhouse cancel instead."));
        }
    }

    private void mine(Player player) {
        List<AuctionListing> listings = auctionHouse.listingsBySeller(player.getUniqueId());
        if (listings.isEmpty()) {
            player.sendMessage(color("&7You have no active listings."));
            return;
        }
        player.sendMessage(color("&6&lYour Listings"));
        for (AuctionListing listing : listings) {
            player.sendMessage(color("&e#" + listing.id() + " &f" + listing.amount() + "x " + displayName(listing)
                    + " &7- " + formatMoney(listing.price())));
        }
    }

    private void cancel(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /auctionhouse cancel <id>"));
            return;
        }
        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(color("&cInvalid listing id."));
            return;
        }
        AuctionListing listing = auctionHouse.find(id);
        if (listing == null) {
            player.sendMessage(color("&cNo listing #" + id + "."));
            return;
        }
        boolean owner = listing.sellerId().equals(player.getUniqueId());
        if (!owner && !player.hasPermission("auctionhouse.admin")) {
            player.sendMessage(color("&cYou don't have a listing #" + id + "."));
            return;
        }
        auctionHouse.cancel(id, owner ? player : org.bukkit.Bukkit.getPlayer(listing.sellerId()));
        player.sendMessage(color("&aListing #" + id + " cancelled"
                + (owner ? " and returned to your inventory." : " and returned to the seller if they're online.")));
    }

    private static String displayName(AuctionListing listing) {
        return listing.displayName() != null ? listing.displayName() : niceMaterialName(listing.material());
    }

    private static String displayName(ItemStack item) {
        var meta = item.getItemMeta();
        return meta != null && meta.getDisplayName() != null ? meta.getDisplayName() : niceMaterialName(item.getType());
    }

    private static String niceMaterialName(Material material) {
        String[] words = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }

    private static String formatMoney(double amount) {
        return "$" + String.format(Locale.US, "%,.2f", amount);
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
