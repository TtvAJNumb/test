package com.donututils.realworld.arsenal.command;

import com.donututils.realworld.RealWorldPlugin;
import com.donututils.realworld.arsenal.config.ArsenalConfig;
import com.donututils.realworld.arsenal.config.WeaponDefinition;
import com.donututils.realworld.arsenal.weapon.WeaponItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public final class ArsenalCommand implements CommandExecutor {

    private final RealWorldPlugin plugin;
    private final WeaponItemFactory itemFactory;

    public ArsenalCommand(RealWorldPlugin plugin, WeaponItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&cUsage: /arsenal [give <player> <weapon>|giveammo <player> <weapon> [amount]|list|reload]"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> give(sender, args);
            case "giveammo" -> giveAmmo(sender, args);
            case "list" -> list(sender);
            case "reload" -> {
                plugin.reloadArsenal();
                sender.sendMessage(color("&aArsenal config reloaded."));
            }
            default -> sender.sendMessage(color("&cUnknown /arsenal subcommand."));
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(color("&cUsage: /arsenal give <player> <weapon>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(color("&c" + args[1] + " isn't online."));
            return;
        }
        WeaponDefinition definition = plugin.getArsenalConfig().weapon(args[2]);
        if (definition == null) {
            sender.sendMessage(color("&cUnknown weapon: " + args[2]));
            return;
        }
        ItemStack weapon = itemFactory.createWeapon(definition);
        target.getInventory().addItem(weapon);
        sender.sendMessage(color("&aGave " + target.getName() + " a " + definition.displayName() + "&a."));
    }

    private void giveAmmo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(color("&cUsage: /arsenal giveammo <player> <weapon> [amount]"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(color("&c" + args[1] + " isn't online."));
            return;
        }
        WeaponDefinition definition = plugin.getArsenalConfig().weapon(args[2]);
        if (definition == null) {
            sender.sendMessage(color("&cUnknown weapon: " + args[2]));
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(color("&cInvalid amount."));
                return;
            }
        }
        ItemStack ammo = itemFactory.createAmmo(definition, amount);
        target.getInventory().addItem(ammo);
        sender.sendMessage(color("&aGave " + target.getName() + " " + amount + "x " + definition.ammoDisplayName() + "&a."));
    }

    private void list(CommandSender sender) {
        ArsenalConfig config = plugin.getArsenalConfig();
        if (config.weapons().isEmpty()) {
            sender.sendMessage(color("&7No weapons configured."));
            return;
        }
        sender.sendMessage(color("&6&lWeapons"));
        for (WeaponDefinition definition : config.weapons().values()) {
            sender.sendMessage(color("&e" + definition.id() + " &7- " + definition.displayName()));
        }
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
