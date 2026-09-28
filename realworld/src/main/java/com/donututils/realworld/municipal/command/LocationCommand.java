package com.donututils.realworld.municipal.command;

import com.donututils.realworld.municipal.location.LocationManager;
import com.donututils.realworld.municipal.location.NamedLocation;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class LocationCommand implements CommandExecutor {

    private final LocationManager locationManager;

    public LocationCommand(LocationManager locationManager) {
        this.locationManager = locationManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&cUsage: /location [pos1|pos2|save <name>|remove <name>|list|info <name>|tp <name>]"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "pos1" -> pos(sender, true);
            case "pos2" -> pos(sender, false);
            case "save" -> save(sender, args);
            case "remove" -> remove(sender, args);
            case "list" -> list(sender);
            case "info" -> info(sender, args);
            case "tp" -> teleport(sender, args);
            default -> sender.sendMessage(color("&cUnknown /location subcommand."));
        }
        return true;
    }

    private void pos(CommandSender sender, boolean first) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (!player.hasPermission("municipal.location.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return;
        }
        if (first) {
            locationManager.setPos1(player);
        } else {
            locationManager.setPos2(player);
        }
        sender.sendMessage(color("&aPos" + (first ? "1" : "2") + " set to your current position."));
    }

    private void save(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (!player.hasPermission("municipal.location.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /location save <name>"));
            return;
        }
        LocationManager.SaveResult result = locationManager.save(args[1], player);
        sender.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
    }

    private void remove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("municipal.location.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /location remove <name>"));
            return;
        }
        if (locationManager.remove(args[1])) {
            sender.sendMessage(color("&aRemoved '" + args[1].toLowerCase(Locale.ROOT) + "'."));
        } else {
            sender.sendMessage(color("&cNo location named '" + args[1] + "'."));
        }
    }

    private void list(CommandSender sender) {
        if (locationManager.all().isEmpty()) {
            sender.sendMessage(color("&7No locations saved yet."));
            return;
        }
        sender.sendMessage(color("&6&lLocations"));
        for (NamedLocation location : locationManager.all()) {
            sender.sendMessage(color("&e" + location.name() + " &7(" + location.world()
                    + (location.hasBounds() ? ", cuboid" : ", point") + ")"));
        }
    }

    private void info(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /location info <name>"));
            return;
        }
        NamedLocation location = locationManager.get(args[1]);
        if (location == null) {
            sender.sendMessage(color("&cNo location named '" + args[1] + "'."));
            return;
        }
        sender.sendMessage(color("&6&l" + location.name()));
        sender.sendMessage(color("&7World: &f" + location.world()));
        if (location.hasBounds()) {
            var b = location.bounds();
            sender.sendMessage(color(String.format(Locale.US, "&7Bounds: &f(%.1f, %.1f, %.1f) to (%.1f, %.1f, %.1f)",
                    b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())));
        } else {
            sender.sendMessage(color("&7Point warp - no cuboid bounds."));
        }
    }

    private void teleport(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /location tp <name>"));
            return;
        }
        NamedLocation location = locationManager.get(args[1]);
        if (location == null) {
            sender.sendMessage(color("&cNo location named '" + args[1] + "'."));
            return;
        }
        Location target = location.toLocation();
        if (target == null) {
            sender.sendMessage(color("&cThat location's world isn't loaded right now."));
            return;
        }
        player.teleport(target);
        sender.sendMessage(color("&aTeleported to '" + location.name() + "'."));
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(color("&cOnly players can do that."));
        return null;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
