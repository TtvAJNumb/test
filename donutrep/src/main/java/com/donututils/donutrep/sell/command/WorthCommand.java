package com.donututils.donutrep.sell.command;

import com.donututils.donutrep.sell.SellService;
import com.donututils.donutrep.sell.WorthStore;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/** /worth [material] - shows the sell price of the item in your hand, or a named material. */
public final class WorthCommand implements CommandExecutor {

    private final WorthStore worthStore;
    private final SellService sellService;

    public WorthCommand(WorthStore worthStore, SellService sellService) {
        this.worthStore = worthStore;
        this.sellService = sellService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Material material;
        if (args.length >= 1) {
            try {
                material = Material.valueOf(args[0].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(color("&cUnknown material '" + args[0] + "'."));
                return true;
            }
        } else if (sender instanceof Player player) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) {
                sender.sendMessage(color("&cYou're not holding anything. Usage: /worth [material]"));
                return true;
            }
            material = hand.getType();
        } else {
            sender.sendMessage(color("&cUsage: /worth <material>"));
            return true;
        }

        if (!worthStore.hasPrice(material)) {
            sender.sendMessage(color("&7Worth: &cthe server doesn't buy " + SellService.displayName(material) + "."));
            return true;
        }
        double price = sellService.unitPrice(material);
        sender.sendMessage(color("&7Worth: &a$" + String.format(Locale.US, "%,.2f", price) + " &7each (" + SellService.displayName(material) + ")"));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
