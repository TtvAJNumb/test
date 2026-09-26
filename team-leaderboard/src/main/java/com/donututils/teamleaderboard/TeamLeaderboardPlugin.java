package com.donututils.teamleaderboard;

import com.donututils.teamleaderboard.command.TeamLeaderboardAdminCommand;
import com.donututils.teamleaderboard.command.TeamLeaderboardCommand;
import com.donututils.teamleaderboard.config.TeamLeaderboardConfig;
import com.donututils.teamleaderboard.gui.MenuClickListener;
import com.donututils.teamleaderboard.model.StatKind;
import com.donututils.teamleaderboard.reflect.UdsBridge;
import com.donututils.teamleaderboard.service.TeamLeaderboardService;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class TeamLeaderboardPlugin extends JavaPlugin {

    private TeamLeaderboardService service;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Plugin uds = getServer().getPluginManager().getPlugin("UltimateDonutSmp");
        if (uds == null || !uds.isEnabled()) {
            getLogger().severe("UltimateDonutSmp is not enabled. Disabling TeamLeaderboard.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        UdsBridge bridge;
        try {
            bridge = UdsBridge.create(uds);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            getLogger().severe("This UltimateDonutSmp version does not expose the team/leaderboard API this add-on needs: " + ex);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        service = new TeamLeaderboardService(bridge, loadConfigValues());

        getServer().getPluginManager().registerEvents(new MenuClickListener(), this);

        registerCommand("teambaltop", new TeamLeaderboardCommand(service, StatKind.MONEY, this::loadConfigValues));
        registerCommand("teamshardstop", new TeamLeaderboardCommand(service, StatKind.SHARDS, this::loadConfigValues));
        registerCommand("teamleaderboard", new TeamLeaderboardAdminCommand(this));

        getLogger().info("TeamLeaderboard enabled, bridged to UltimateDonutSmp.");
    }

    public void reloadTeamLeaderboard() {
        reloadConfig();
        if (service != null) {
            service.updateConfig(loadConfigValues());
        }
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        }
    }

    private TeamLeaderboardConfig loadConfigValues() {
        FileConfiguration c = getConfig();
        return new TeamLeaderboardConfig(
                Math.max(1, c.getInt("refresh-interval-seconds", 5)),
                c.getString("messages.no-teams", "&7No teams have any members yet."),
                c.getString("messages.not-in-a-team-line", "&7You are not currently in a team.")
        );
    }
}
