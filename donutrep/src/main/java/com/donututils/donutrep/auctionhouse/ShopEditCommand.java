package com.donututils.donutrep.auctionhouse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /shopedit remove <id> - staff moderation for the Auction House: force-removes any listing,
 * returning the item to its seller if they're online. */
public final class ShopEditCommand implements CommandExecutor {

    private final AuctionHouseManager auctionHouse;

    public ShopEditCommand(AuctionHouseManager auctionHouse) {
        this.auctionHouse = auctionHouse;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("auctionhouse.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return true;
        }
        if (args.length < 2 || !args[0].equalsIgnoreCase("remove")) {
            sender.sendMessage(color("&cUsage: /shopedit remove <id>"));
            return true;
        }
        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(color("&cInvalid listing id."));
            return true;
        }
        AuctionListing listing = auctionHouse.find(id);
        if (listing == null) {
            sender.sendMessage(color("&cNo listing #" + id + "."));
            return true;
        }
        Player seller = Bukkit.getPlayer(listing.sellerId());
        boolean removed = auctionHouse.cancel(id, seller);
        if (removed) {
            sender.sendMessage(color("&aRemoved listing #" + id + (seller != null ? " and returned it to " + seller.getName() + "." : " (seller offline - not returned).")));
        } else {
            sender.sendMessage(color("&cCouldn't remove that listing."));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
