package com.donututils.donutrep.homes;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class RenameHomeCommand implements CommandExecutor {

    private final HomeManager homeManager;

    public RenameHomeCommand(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /renamehome <old> <new>"));
            return true;
        }
        HomeManager.Result result = homeManager.renameHome(player.getUniqueId(), args[0], args[1]);
        switch (result) {
            case UPDATED -> player.sendMessage(color("&aRenamed '" + args[0] + "' to '" + args[1] + "'."));
            case NOT_FOUND -> player.sendMessage(color("&cNo home named '" + args[0] + "'."));
            case ALREADY_EXISTS -> player.sendMessage(color("&cYou already have a home named '" + args[1] + "'."));
            default -> player.sendMessage(color("&cCouldn't rename that home."));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
