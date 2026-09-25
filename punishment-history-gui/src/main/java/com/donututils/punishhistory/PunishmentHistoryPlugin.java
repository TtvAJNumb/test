package com.donututils.punishhistory;

import com.donututils.punishhistory.command.NoteCommand;
import com.donututils.punishhistory.command.PunishHistoryCommand;
import com.donututils.punishhistory.config.PunishConfig;
import com.donututils.punishhistory.gui.MenuClickListener;
import com.donututils.punishhistory.notes.NoteStore;
import com.donututils.punishhistory.reflect.UdsBridge;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class PunishmentHistoryPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Plugin uds = getServer().getPluginManager().getPlugin("UltimateDonutSmp");
        if (uds == null || !uds.isEnabled()) {
            getLogger().severe("UltimateDonutSmp is not enabled. Disabling PunishmentHistoryGUI.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        UdsBridge bridge;
        try {
            bridge = UdsBridge.create(uds);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            getLogger().severe("This UltimateDonutSmp version does not expose the punishment API this add-on needs: " + ex);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        int maxNotes = getConfig().getInt("max-notes-per-player", 50);
        NoteStore noteStore = new NoteStore(this, maxNotes);
        PunishConfig config = new PunishConfig(
                getConfig().getString("gui.roster-title", "&5&lPunishment Roster"),
                getConfig().getString("gui.detail-title", "&5&lHistory: %player%"),
                getConfig().getInt("roster-scan-size", 200),
                maxNotes
        );

        getServer().getPluginManager().registerEvents(new MenuClickListener(), this);

        PunishHistoryCommand punishHistory = new PunishHistoryCommand(bridge, noteStore, config);
        PluginCommand punishHistoryCommand = getCommand("punishhistory");
        if (punishHistoryCommand != null) {
            punishHistoryCommand.setExecutor(punishHistory);
            punishHistoryCommand.setTabCompleter(punishHistory);
        }

        NoteCommand noteCommand = new NoteCommand(noteStore, bridge);
        PluginCommand noteCmd = getCommand("note");
        if (noteCmd != null) {
            noteCmd.setExecutor(noteCommand);
            noteCmd.setTabCompleter(noteCommand);
        }

        getLogger().info("Ready. Punishment data is read live from UltimateDonutSmp; notes are stored locally.");
    }
}
