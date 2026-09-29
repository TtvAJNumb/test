package com.donututils.donutrep.market.command;

import com.donututils.donutrep.market.service.MarketService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** /sellhand [amount] - sells the item currently in your hand. */
public final class SellHandCommand implements CommandExecutor {

    private final MarketService marketService;

    public SellHandCommand(MarketService marketService) {
        this.marketService = marketService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        MarketService.TradeResult result;
        if (args.length >= 1) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) {
                player.sendMessage(color("&cYou're not holding anything."));
                return true;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                player.sendMessage(color("&cInvalid amount."));
                return true;
            }
            result = marketService.sell(player, hand.getType(), amount);
        } else {
            result = marketService.sellHand(player);
        }
        player.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
