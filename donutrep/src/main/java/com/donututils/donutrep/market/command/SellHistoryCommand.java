package com.donututils.donutrep.market.command;

import com.donututils.donutrep.market.SellLedger;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** /sellhistory - your most recent sell transactions. */
public final class SellHistoryCommand implements CommandExecutor {

    private static final int LIMIT = 10;

    private final SellLedger sellLedger;

    public SellHistoryCommand(SellLedger sellLedger) {
        this.sellLedger = sellLedger;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        List<SellLedger.Entry> entries = sellLedger.recentSales(player.getUniqueId(), LIMIT);
        if (entries.isEmpty()) {
            player.sendMessage(color("&7You haven't sold anything yet."));
            return true;
        }
        player.sendMessage(color("&6&lRecent Sales"));
        for (SellLedger.Entry entry : entries) {
            player.sendMessage(color("&e" + entry.amount() + "x " + entry.material().name()
                    + " &7- $" + String.format(Locale.US, "%,.2f", entry.totalPrice())));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
