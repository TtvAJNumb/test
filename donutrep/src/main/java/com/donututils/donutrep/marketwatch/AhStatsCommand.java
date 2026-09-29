package com.donututils.donutrep.marketwatch;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class AhStatsCommand implements CommandExecutor {

    private final MarketWatchCommand delegate;

    public AhStatsCommand(MarketWatchCommand delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return delegate.onAhStats(sender, args);
    }
}
