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

/** /orders - the real player-to-player marketplace:
 * /orders sell &lt;price&gt; - lists the item in your hand
 * /orders list [page] - browses every active listing
 * /orders buy &lt;id&gt; - buys a listing
 * /orders mine - lists your own active listings
 * /orders cancel &lt;id&gt; - pulls your own listing, returning the item */
public final class OrdersCommand implements CommandExecutor {

    private static final int PAGE_SIZE = 8;

    private final AuctionHouseManager auctionHouse;

    public OrdersCommand(AuctionHouseManager auctionHouse) {
        this.auctionHouse = auctionHouse;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(color("&cUsage: /orders <sell <price>|list [page]|buy <id>|mine|cancel <id>>"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "sell" -> sell(player, args);
            case "list" -> list(player, args);
            case "buy" -> buy(player, args);
            case "mine" -> mine(player);
            case "cancel" -> cancel(player, args);
            default -> player.sendMessage(color("&cUsage: /orders <sell <price>|list [page]|buy <id>|mine|cancel <id>>"));
        }
        return true;
    }

    private void sell(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /orders sell <price>"));
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
                    + " &7- " + formatMoney(listing.price()) + " &7(by " + listing.sellerName() + ") &8/orders buy " + listing.id()));
        }
    }

    private void buy(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /orders buy <id>"));
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
            case OWN_LISTING -> player.sendMessage(color("&cYou can't buy your own listing - use /orders cancel instead."));
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
            player.sendMessage(color("&cUsage: /orders cancel <id>"));
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
        if (listing == null || !listing.sellerId().equals(player.getUniqueId())) {
            player.sendMessage(color("&cYou don't have a listing #" + id + "."));
            return;
        }
        auctionHouse.cancel(id, player);
        player.sendMessage(color("&aListing #" + id + " cancelled and returned to your inventory."));
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
