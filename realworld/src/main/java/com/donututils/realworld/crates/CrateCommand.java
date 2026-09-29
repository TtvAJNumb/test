package com.donututils.realworld.crates;

import com.donututils.realworld.RealWorldPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class CrateCommand implements CommandExecutor {

    private final RealWorldPlugin plugin;
    private final CrateManager crateManager;

    public CrateCommand(RealWorldPlugin plugin, CrateManager crateManager) {
        this.plugin = plugin;
        this.crateManager = crateManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&cUsage: /crate <bind <id>|unbind|set <player> <crate> <amount>|give <player> <crate> <amount>|list|reload>"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "bind" -> bind(sender, args);
            case "unbind" -> unbind(sender);
            case "set", "give" -> giveKeys(sender, args);
            case "list" -> list(sender);
            case "reload" -> reload(sender);
            default -> sender.sendMessage(color("&cUnknown /crate subcommand."));
        }
        return true;
    }

    private void bind(CommandSender sender, String[] args) {
        Player player = requirePermissionedPlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /crate bind <crate>"));
            return;
        }
        Block target = player.getTargetBlockExact(8);
        if (target == null || !crateManager.isBindable(target.getType())) {
            player.sendMessage(color("&cLook at a chest, trapped chest, barrel, ender chest, or shulker box within 8 blocks first."));
            return;
        }
        if (plugin.getCrateConfig().crate(args[1]) == null) {
            player.sendMessage(color("&cNo crate named &f" + args[1] + "&c."));
            return;
        }
        String existing = crateManager.boundCrateId(target);
        if (args[1].equalsIgnoreCase(existing)) {
            player.sendMessage(color("&eThat block is already bound to &f" + existing + "&e."));
            return;
        }
        crateManager.bind(target, args[1].toLowerCase(Locale.ROOT));
        player.sendMessage(color("&aBound this block to &f" + args[1] + "&a. Right click it with a matching key to open it."));
    }

    private void unbind(CommandSender sender) {
        Player player = requirePermissionedPlayer(sender);
        if (player == null) {
            return;
        }
        Block target = player.getTargetBlockExact(8);
        if (target == null) {
            player.sendMessage(color("&cLook at a bound crate block within 8 blocks first."));
            return;
        }
        String existing = crateManager.boundCrateId(target);
        if (existing == null) {
            player.sendMessage(color("&cThat block is not bound to any crate."));
            return;
        }
        crateManager.unbind(target);
        player.sendMessage(color("&aUnbound that block from &f" + existing + "&a."));
    }

    @SuppressWarnings("deprecation")
    private void giveKeys(CommandSender sender, String[] args) {
        if (!sender.hasPermission("crates.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(color("&cUsage: /crate set <player> <crate> <amount>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        Player online = target.getName() != null ? Bukkit.getPlayer(target.getUniqueId()) : null;
        if (online == null) {
            sender.sendMessage(color("&c" + args[1] + " must be online to receive keys."));
            return;
        }
        if (plugin.getCrateConfig().crate(args[2]) == null) {
            sender.sendMessage(color("&cNo crate named &f" + args[2] + "&c."));
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(color("&cInvalid amount."));
            return;
        }
        crateManager.giveKeys(online, args[2].toLowerCase(Locale.ROOT), amount);
        sender.sendMessage(color("&aGave " + amount + "x " + args[2] + " key(s) to " + args[1] + "."));
    }

    private void list(CommandSender sender) {
        sender.sendMessage(color("&6&lCrates"));
        for (CrateDefinition crate : plugin.getCrateConfig().crates().values()) {
            sender.sendMessage(color("&e" + crate.id() + " &7- " + crate.displayName() + " (" + crate.rewards().size() + " reward(s))"));
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("crates.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return;
        }
        plugin.reloadCrates();
        sender.sendMessage(color("&aCrates config reloaded."));
    }

    private Player requirePermissionedPlayer(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return null;
        }
        if (!player.hasPermission("crates.admin")) {
            player.sendMessage(color("&cYou do not have permission to do that."));
            return null;
        }
        return player;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
