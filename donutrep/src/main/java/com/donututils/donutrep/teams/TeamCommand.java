package com.donututils.donutrep.teams;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

public final class TeamCommand implements CommandExecutor {

    private final TeamManager teamManager;

    public TeamCommand(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&cUsage: /team <create <name>|disband|add <player>|remove <player>|leave|info [player]|list>"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> create(sender, args);
            case "disband" -> disband(sender);
            case "add", "invite" -> addMember(sender, args);
            case "remove", "kick" -> removeMember(sender, args);
            case "leave" -> leave(sender);
            case "info" -> info(sender, args);
            case "list" -> list(sender);
            default -> sender.sendMessage(color("&cUnknown /team subcommand."));
        }
        return true;
    }

    private void create(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /team create <name>"));
            return;
        }
        TeamManager.Result result = teamManager.createTeam(player.getUniqueId(), args[1]);
        player.sendMessage(color(switch (result) {
            case OK -> "&aTeam '" + args[1] + "' created - you're the leader.";
            case ALREADY_IN_TEAM -> "&cYou're already in a team - leave it first.";
            case NAME_TAKEN -> "&cThat team name is already taken.";
            default -> "&cCould not create the team.";
        }));
    }

    private void disband(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        TeamManager.Result result = teamManager.disband(player.getUniqueId());
        player.sendMessage(color(switch (result) {
            case OK -> "&aTeam disbanded.";
            case NOT_LEADER -> "&cOnly the team leader can disband it.";
            case NOT_A_MEMBER -> "&cYou're not in a team.";
            default -> "&cCould not disband the team.";
        }));
    }

    @SuppressWarnings("deprecation")
    private void addMember(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /team add <player>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        TeamManager.Result result = teamManager.addMember(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(color(switch (result) {
            case OK -> "&aAdded " + args[1] + " to your team.";
            case NOT_LEADER -> "&cOnly the team leader can add members.";
            case NOT_A_MEMBER -> "&cYou're not in a team - create one first.";
            case TARGET_IN_TEAM -> "&c" + args[1] + " is already in a team.";
            default -> "&cCould not add that player.";
        }));
    }

    @SuppressWarnings("deprecation")
    private void removeMember(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /team remove <player>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        TeamManager.Result result = teamManager.removeMember(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(color(switch (result) {
            case OK -> "&aRemoved " + args[1] + " from your team.";
            case NOT_LEADER -> "&cOnly the team leader can remove members.";
            case NOT_A_MEMBER -> "&cYou're not in a team.";
            case NOT_FOUND -> "&c" + args[1] + " isn't in your team.";
            default -> "&cCould not remove that player.";
        }));
    }

    private void leave(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        TeamManager.Result result = teamManager.leave(player.getUniqueId());
        player.sendMessage(color(switch (result) {
            case OK -> "&aYou left the team.";
            case NOT_A_MEMBER -> "&cYou're not in a team.";
            default -> "&cCould not leave the team.";
        }));
    }

    @SuppressWarnings("deprecation")
    private void info(CommandSender sender, String[] args) {
        UUID targetId;
        String label;
        if (args.length >= 2) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            targetId = target.getUniqueId();
            label = args[1] + "'s";
        } else {
            Player player = requirePlayer(sender);
            if (player == null) {
                return;
            }
            targetId = player.getUniqueId();
            label = "Your";
        }
        Team team = teamManager.teamOf(targetId);
        if (team == null) {
            sender.sendMessage(color("&7" + label + " team: &cnone"));
            return;
        }
        sender.sendMessage(color("&6&l" + team.name()));
        sender.sendMessage(color("&7Leader: &f" + Bukkit.getOfflinePlayer(team.leaderId()).getName()));
        sender.sendMessage(color("&7Members: &f" + team.memberIds().size()));
    }

    private void list(CommandSender sender) {
        sender.sendMessage(color("&6&lTeams"));
        if (teamManager.allTeams().isEmpty()) {
            sender.sendMessage(color("&7No teams yet."));
            return;
        }
        for (Team team : teamManager.allTeams()) {
            sender.sendMessage(color("&e" + team.name() + " &7(" + team.memberIds().size() + " member(s))"));
        }
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
