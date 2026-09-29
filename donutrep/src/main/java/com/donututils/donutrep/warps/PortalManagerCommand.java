package com.donututils.donutrep.warps;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** /portalmanager &lt;pos1|pos2|create <destination-warp>|delete <id>|list&gt; - admin tool defining
 * physical portal regions that teleport players to a warp on contact. */
public final class PortalManagerCommand implements CommandExecutor {

    private final PortalManager portalManager;

    public PortalManagerCommand(PortalManager portalManager) {
        this.portalManager = portalManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("warps.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        if (args.length < 1) {
            sendUsage(player);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "pos1" -> {
                portalManager.setPos1(player.getUniqueId(), player.getLocation());
                player.sendMessage(color("&aPosition 1 set."));
            }
            case "pos2" -> {
                portalManager.setPos2(player.getUniqueId(), player.getLocation());
                player.sendMessage(color("&aPosition 2 set."));
            }
            case "create" -> {
                if (args.length < 2) {
                    player.sendMessage(color("&cUsage: /portalmanager create <destination-warp>"));
                    return true;
                }
                String error = portalManager.create(player.getUniqueId(), args[1]);
                if (error != null) {
                    player.sendMessage(color("&c" + error));
                } else {
                    player.sendMessage(color("&aPortal created, linked to warp '" + args[1] + "'."));
                }
            }
            case "delete" -> {
                if (args.length < 2) {
                    player.sendMessage(color("&cUsage: /portalmanager delete <id>"));
                    return true;
                }
                try {
                    int id = Integer.parseInt(args[1]);
                    player.sendMessage(color(portalManager.delete(id) ? "&aDeleted portal #" + id + "." : "&cNo portal #" + id + "."));
                } catch (NumberFormatException ex) {
                    player.sendMessage(color("&cInvalid portal id."));
                }
            }
            case "list" -> {
                List<PortalManager.Portal> portals = portalManager.allPortals();
                if (portals.isEmpty()) {
                    player.sendMessage(color("&7No portals defined."));
                } else {
                    player.sendMessage(color("&6&lPortals"));
                    for (PortalManager.Portal portal : portals) {
                        player.sendMessage(color("&e#" + portal.id() + " &7-> &f" + portal.destinationWarp() + " &7(" + portal.world().getName() + ")"));
                    }
                }
            }
            default -> sendUsage(player);
        }
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(color("&cUsage: /portalmanager <pos1|pos2|create <warp>|delete <id>|list>"));
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
