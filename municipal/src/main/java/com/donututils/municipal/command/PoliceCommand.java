package com.donututils.municipal.command;

import com.donututils.municipal.court.CourtManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class PoliceCommand implements CommandExecutor {

    private final Plugin plugin;
    private final CourtManager courtManager;

    public PoliceCommand(Plugin plugin, CourtManager courtManager) {
        this.plugin = plugin;
        this.courtManager = courtManager;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player officer)) {
            sender.sendMessage(color("&cOnly players can make arrests."));
            return true;
        }
        if (args.length < 2 || !args[0].equalsIgnoreCase("arrest")) {
            officer.sendMessage(color("&cUsage: /police arrest <player> <reason>"));
            return true;
        }
        OfflinePlayer defendant = Bukkit.getOfflinePlayer(args[1]);
        if (!defendant.hasPlayedBefore() && Bukkit.getServer().getPlayer(defendant.getUniqueId()) == null) {
            officer.sendMessage(color("&cUnknown player: " + args[1]));
            return true;
        }
        String reason = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "No reason given";

        // CourtManager#arrest blocks on a database insert to get the case's id back - never call it
        // directly from a command handler on the main thread.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            var courtCase = courtManager.arrest(officer.getUniqueId(), defendant.getUniqueId(), reason);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (courtCase == null) {
                    officer.sendMessage(color("&cSomething went wrong recording the arrest - try again."));
                    return;
                }
                officer.sendMessage(color("&aArrested " + defendant.getName() + " - case #" + courtCase.id() + " is now pending trial."));
                Player onlineDefendant = Bukkit.getServer().getPlayer(defendant.getUniqueId());
                if (onlineDefendant != null) {
                    onlineDefendant.sendMessage(color("&cYou've been arrested: " + reason + " &7(case #" + courtCase.id() + ", pending trial)"));
                }
            });
        });
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
