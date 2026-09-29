package com.donututils.donutrep.sell.command;

import com.donututils.donutrep.sell.SellLedger;
import com.donututils.donutrep.sell.SellService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

/** /topsell - the top sellers leaderboard by lifetime Money earned. */
public final class TopSellCommand implements CommandExecutor {

    private final SellLedger ledger;

    public TopSellCommand(SellLedger ledger) {
        this.ledger = ledger;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<SellLedger.TopSeller> top = ledger.topSellers(10);
        if (top.isEmpty()) {
            sender.sendMessage(color("&7Nobody has sold anything yet."));
            return true;
        }
        sender.sendMessage(color("&6&lTop Sellers"));
        int rank = 1;
        for (SellLedger.TopSeller entry : top) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.playerId());
            String name = player.getName() != null ? player.getName() : entry.playerId().toString();
            sender.sendMessage(color("&e#" + rank + " &f" + name + " &7- " + SellService.formatMoney(entry.moneyEarned())));
            rank++;
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
