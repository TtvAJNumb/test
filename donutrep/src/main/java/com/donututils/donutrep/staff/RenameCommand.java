package com.donututils.donutrep.staff;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** /rename <name...> - renames the item in your hand (color codes allowed). Pass "reset" to clear a
 * custom name back to the item's default. */
public final class RenameCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(color("&cUsage: /rename <name>"));
            return true;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            player.sendMessage(color("&cYou're not holding anything."));
            return true;
        }
        ItemMeta meta = hand.getItemMeta();
        if (meta == null) {
            player.sendMessage(color("&cThat item can't be renamed."));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reset")) {
            meta.setDisplayName(null);
            hand.setItemMeta(meta);
            player.sendMessage(color("&aName reset."));
            return true;
        }
        String name = color(String.join(" ", args));
        meta.setDisplayName(name);
        hand.setItemMeta(meta);
        player.sendMessage(color("&aRenamed your item."));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
