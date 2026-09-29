package com.donututils.donutrep.homes;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SetHomeCommand implements CommandExecutor {

    private final HomeManager homeManager;

    public SetHomeCommand(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        String name = args.length >= 1 ? args[0] : "home";
        HomeManager.Result result = homeManager.setHome(player.getUniqueId(), name, player.getLocation());
        switch (result) {
            case CREATED -> player.sendMessage(color("&aSet home '" + name + "'."));
            case UPDATED -> player.sendMessage(color("&aUpdated home '" + name + "'."));
            case LIMIT_REACHED -> player.sendMessage(color("&cYou've reached your home limit. Delete one with /delhome <name> first."));
            default -> player.sendMessage(color("&cCouldn't set that home."));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
