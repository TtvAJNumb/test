package com.donututils.crateodds;

import com.donututils.crateodds.command.CrateHistoryCommand;
import com.donututils.crateodds.command.CrateOddsCommand;
import com.donututils.crateodds.gui.MenuClickListener;
import com.donututils.crateodds.history.HistoryStore;
import com.donututils.crateodds.listener.OpenWatcher;
import com.donututils.crateodds.reflect.UdsBridge;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class CrateOddsHistoryPlugin extends JavaPlugin {

    private OpenWatcher openWatcher;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Plugin uds = getServer().getPluginManager().getPlugin("UltimateDonutSmp");
        if (uds == null || !uds.isEnabled()) {
            getLogger().severe("UltimateDonutSmp is not enabled. Disabling CrateOddsHistory.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        UdsBridge bridge;
        try {
            bridge = UdsBridge.create(uds);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            getLogger().severe("This UltimateDonutSmp version does not expose the crate API this add-on needs: " + ex);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        int historySize = getConfig().getInt("history-size", 20);
        int pollInterval = getConfig().getInt("poll-interval-ticks", 2);
        int watchTimeout = getConfig().getInt("watch-timeout-ticks", 600);
        String oddsTitle = getConfig().getString("gui.odds-title", "&6&lCrate Odds");
        String historyTitle = getConfig().getString("gui.history-title", "&6&lCrate History");

        HistoryStore historyStore = new HistoryStore(this, historySize);
        openWatcher = new OpenWatcher(this, bridge, historyStore, pollInterval, watchTimeout);

        getServer().getPluginManager().registerEvents(openWatcher, this);
        getServer().getPluginManager().registerEvents(new MenuClickListener(), this);

        CrateOddsCommand oddsCommand = new CrateOddsCommand(bridge, oddsTitle);
        var oddsPluginCommand = getCommand("crateodds");
        if (oddsPluginCommand != null) {
            oddsPluginCommand.setExecutor(oddsCommand);
            oddsPluginCommand.setTabCompleter(oddsCommand);
        }

        CrateHistoryCommand historyCommand = new CrateHistoryCommand(historyStore, historyTitle);
        var historyPluginCommand = getCommand("cratehistory");
        if (historyPluginCommand != null) {
            historyPluginCommand.setExecutor(historyCommand);
            historyPluginCommand.setTabCompleter(historyCommand);
        }

        getLogger().info("Ready. Bound crate blocks are read live from UltimateDonutSmp/CrateBindAddon.");
    }

    @Override
    public void onDisable() {
        if (openWatcher != null) {
            openWatcher.shutdown();
        }
    }
}
