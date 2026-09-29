package com.donututils.donutrep.market.command;

import com.donututils.donutrep.market.SellLedger;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;

/** /topsell - the top 10 players by lifetime Money earned from selling to the Market. */
public final class TopSellCommand implements CommandExecutor {

    private static final int LIMIT = 10;

    private final SellLedger sellLedger;

    public TopSellCommand(SellLedger sellLedger) {
        this.sellLedger = sellLedger;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<SellLedger.TopSeller> top = sellLedger.topSellers(LIMIT);
        if (top.isEmpty()) {
            sender.sendMessage(color("&7No sales recorded yet."));
            return true;
        }
        sender.sendMessage(color("&6&lTop Sellers"));
        int rank = 1;
        for (SellLedger.TopSeller seller : top) {
            sender.sendMessage(color("&e#" + rank + " &f" + seller.playerName() + " &7- $"
                    + String.format(Locale.US, "%,.2f", seller.totalEarned())));
            rank++;
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
