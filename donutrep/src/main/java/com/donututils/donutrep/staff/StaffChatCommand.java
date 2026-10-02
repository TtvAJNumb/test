package com.donututils.donutrep.staff;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /staffchat <message> broadcasts once to everyone with staff.staffchat; /staffchat with no args
 * toggles "always staffchat" mode (every chat line routes to staff chat instead of public). */
public final class StaffChatCommand implements CommandExecutor {

    private final StaffChatManager staffChatManager;

    public StaffChatCommand(StaffChatManager staffChatManager) {
        this.staffChatManager = staffChatManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(color("&cUsage: /staffchat <message>"));
                return true;
            }
            boolean nowOn = staffChatManager.toggleAlwaysOn(player.getUniqueId());
            player.sendMessage(color(nowOn ? "&7Staff chat mode enabled - your messages now go to staff only."
                    : "&aStaff chat mode disabled - your messages go to public chat again."));
            return true;
        }
        String message = String.join(" ", args);
        broadcast(sender.getName(), message);
        return true;
    }

    public void broadcast(String senderName, String message) {
        String formatted = color("&5&l[Staff] &d" + senderName + "&7: &f" + message);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("staff.staffchat")) {
                online.sendMessage(formatted);
            }
        }
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
