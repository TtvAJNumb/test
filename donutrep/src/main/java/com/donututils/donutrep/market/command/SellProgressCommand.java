package com.donututils.donutrep.market.command;

import com.donututils.donutrep.market.SellLedger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/** /sellprogress [player] - lifetime selling stats: total items sold and total Money earned from
 * selling. */
public final class SellProgressCommand implements CommandExecutor {

    private final SellLedger sellLedger;

    public SellProgressCommand(SellLedger sellLedger) {
        this.sellLedger = sellLedger;
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
        long items = sellLedger.lifetimeItemsSold(target.getUniqueId());
        double earned = sellLedger.lifetimeEarnings(target.getUniqueId());
        String whose = args.length >= 1 ? target.getName() + "'s" : "Your";
        sender.sendMessage(color("&6&l" + whose + " Selling Progress"));
        sender.sendMessage(color("&7Items sold: &f" + items));
        sender.sendMessage(color("&7Total earned: &f$" + String.format(Locale.US, "%,.2f", earned)));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
