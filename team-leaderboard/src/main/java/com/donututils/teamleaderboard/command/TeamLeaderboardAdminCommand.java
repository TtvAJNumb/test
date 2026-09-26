package com.donututils.teamleaderboard.command;

import com.donututils.teamleaderboard.TeamLeaderboardPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class TeamLeaderboardAdminCommand implements CommandExecutor {

    private final TeamLeaderboardPlugin plugin;

    public TeamLeaderboardAdminCommand(TeamLeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /teamleaderboard <reload|status>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadTeamLeaderboard();
                sender.sendMessage("TeamLeaderboard config reloaded.");
            }
            case "status" -> sender.sendMessage("TeamLeaderboard is running, bridged to UltimateDonutSmp.");
            default -> sender.sendMessage("Usage: /teamleaderboard <reload|status>");
        }
        return true;
    }
}
