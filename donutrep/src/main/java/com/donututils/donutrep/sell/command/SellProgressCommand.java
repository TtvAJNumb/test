package com.donututils.donutrep.sell.command;

import com.donututils.donutrep.sell.SellLedger;
import com.donututils.donutrep.sell.SellService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /sellprogress [player] - lifetime selling stats: items sold and Money earned. */
public final class SellProgressCommand implements CommandExecutor {

    private final SellLedger ledger;

    public SellProgressCommand(SellLedger ledger) {
        this.ledger = ledger;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        OfflinePlayer target;
        if (args.length >= 1) {
            target = Bukkit.getOfflinePlayer(args[0]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(color("&cUsage: /sellprogress <player>"));
            return true;
        }
        SellLedger.TotalsRecord totals = ledger.totalsFor(target.getUniqueId());
        String who = args.length >= 1 ? (target.getName() + "'s") : "Your";
        sender.sendMessage(color("&6&l" + who + " Sell Progress"));
        sender.sendMessage(color("&7Items sold: &f" + totals.itemsSold()));
        sender.sendMessage(color("&7Money earned: &f" + SellService.formatMoney(totals.moneyEarned())));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
