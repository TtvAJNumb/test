package com.donututils.donutrep.sell.command;

import com.donututils.donutrep.sell.SellLedger;
import com.donututils.donutrep.sell.SellService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** /sellhistory - your most recent sell transactions. */
public final class SellHistoryCommand implements CommandExecutor {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.US).withZone(ZoneId.systemDefault());

    private final SellLedger ledger;

    public SellHistoryCommand(SellLedger ledger) {
        this.ledger = ledger;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        List<SellLedger.SaleRecord> history = ledger.recentHistory(player.getUniqueId(), 10);
        if (history.isEmpty()) {
            player.sendMessage(color("&7You haven't sold anything yet."));
            return true;
        }
        player.sendMessage(color("&6&lYour Recent Sales"));
        for (SellLedger.SaleRecord record : history) {
            Material material;
            try {
                material = Material.valueOf(record.material());
            } catch (IllegalArgumentException ex) {
                material = null;
            }
            String name = material != null ? SellService.displayName(material) : record.material();
            player.sendMessage(color("&7" + FORMAT.format(Instant.ofEpochMilli(record.soldAtMillis())) + " &f- "
                    + record.amount() + "x " + name + " &7for &f" + SellService.formatMoney(record.price())));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
