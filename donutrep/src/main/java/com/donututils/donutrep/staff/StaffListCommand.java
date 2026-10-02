package com.donututils.donutrep.staff;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /stafflist - every online staff member (anyone with staff.command.sus), with their vanish/staffmode
 * status. */
public final class StaffListCommand implements CommandExecutor {

    private final VanishManager vanishManager;
    private final StaffModeManager staffModeManager;

    public StaffListCommand(VanishManager vanishManager, StaffModeManager staffModeManager) {
        this.vanishManager = vanishManager;
        this.staffModeManager = staffModeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean any = false;
        sender.sendMessage(color("&6&lOnline Staff"));
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("staff.command.sus")) {
                continue;
            }
            any = true;
            StringBuilder flags = new StringBuilder();
            if (vanishManager.isVanished(online.getUniqueId())) {
                flags.append(" &7(vanished)");
            }
            if (staffModeManager.isInStaffMode(online.getUniqueId())) {
                flags.append(" &7(staffmode)");
            }
            sender.sendMessage(color("&f- " + online.getName() + flags));
        }
        if (!any) {
            sender.sendMessage(color("&7No staff are online right now."));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
