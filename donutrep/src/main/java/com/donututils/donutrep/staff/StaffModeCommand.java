package com.donututils.donutrep.staff;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class StaffModeCommand implements CommandExecutor {

    private final StaffModeManager staffModeManager;

    public StaffModeCommand(StaffModeManager staffModeManager) {
        this.staffModeManager = staffModeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        boolean nowOn = staffModeManager.toggle(player);
        player.sendMessage(color(nowOn ? "&7Staff mode enabled - inventory stashed, creative + vanish + flight on."
                : "&aStaff mode disabled - your inventory and state are restored."));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
