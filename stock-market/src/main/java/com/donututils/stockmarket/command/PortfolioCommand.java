package com.donututils.stockmarket.command;

import com.donututils.stockmarket.gui.StockMenus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PortfolioCommand implements CommandExecutor {

    private final StockMenus menus;

    public PortfolioCommand(StockMenus menus) {
        this.menus = menus;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        menus.openPortfolio(player);
        return true;
    }
}
