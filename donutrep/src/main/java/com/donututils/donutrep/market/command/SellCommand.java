package com.donututils.donutrep.market.command;

import com.donututils.donutrep.market.gui.MarketGuiService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /sell - opens the sell/shop menu. DonutREP doesn't build a separate sell-only GUI from the
 * catalog shop (real UDS's own /sell menu is presumably a similar item-price browser) - this opens
 * the same Market GUI /shop does, where every item can be shift-click sold. */
public final class SellCommand implements CommandExecutor {

    private final MarketGuiService guiService;

    public SellCommand(MarketGuiService guiService) {
        this.guiService = guiService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        guiService.openRoot(player);
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
