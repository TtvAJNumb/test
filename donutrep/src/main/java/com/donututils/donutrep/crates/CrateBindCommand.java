package com.donututils.donutrep.crates;

import com.donututils.donutrep.DonutREPPlugin;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/** /cratebind <bind <id>|unbind> - the original CrateBindAddon feature: binds an existing chest,
 * trapped chest, barrel, ender chest, or shulker box in the world to a crate, so right-clicking it with
 * a matching key opens it. Split out from /crate (which now matches real UDS's own crate-definition
 * command) so the two don't collide under one name. */
public final class CrateBindCommand implements CommandExecutor {

    private final DonutREPPlugin plugin;
    private final CrateManager crateManager;

    public CrateBindCommand(DonutREPPlugin plugin, CrateManager crateManager) {
        this.plugin = plugin;
        this.crateManager = crateManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&cUsage: /cratebind <bind <id>|unbind>"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "bind" -> bind(sender, args);
            case "unbind" -> unbind(sender);
            default -> sender.sendMessage(color("&cUnknown /cratebind subcommand."));
        }
        return true;
    }

    private void bind(CommandSender sender, String[] args) {
        Player player = requirePermissionedPlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /cratebind bind <crate>"));
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

    private Player requirePermissionedPlayer(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return null;
        }
        if (!player.hasPermission("crates.bind.admin")) {
            player.sendMessage(color("&cYou do not have permission to do that."));
            return null;
        }
        return player;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
