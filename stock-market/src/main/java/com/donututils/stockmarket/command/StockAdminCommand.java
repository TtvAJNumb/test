package com.donututils.stockmarket.command;

import com.donututils.stockmarket.StockMarketPlugin;
import com.donututils.stockmarket.engine.StockRegistry;
import com.donututils.stockmarket.gui.PagedMenu;
import com.donututils.stockmarket.service.MarketAdminService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Locale;

public final class StockAdminCommand implements CommandExecutor {

    private static final String USAGE = "&cUsage: /stockadmin <create|delist|split|halt|resume|event|reload|save> ...";

    private final StockMarketPlugin plugin;
    private final StockRegistry registry;
    private final MarketAdminService adminService;

    public StockAdminCommand(StockMarketPlugin plugin, StockRegistry registry, MarketAdminService adminService) {
        this.plugin = plugin;
        this.registry = registry;
        this.adminService = adminService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(PagedMenu.legacy(USAGE));
            return true;
        }

        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "create" -> create(sender, args);
                case "delist" -> delist(sender, args);
                case "split" -> split(sender, args);
                case "halt" -> halt(sender, args);
                case "resume" -> resume(sender, args);
                case "event" -> event(sender, args);
                case "reload" -> {
                    plugin.reloadStockMarket();
                    sender.sendMessage(PagedMenu.legacy("&aStockMarket config reloaded."));
                }
                case "save" -> {
                    registry.saveAll();
                    sender.sendMessage(PagedMenu.legacy("&aStock data saved."));
                }
                default -> sender.sendMessage(PagedMenu.legacy(USAGE));
            }
        } catch (NumberFormatException ex) {
            sender.sendMessage(PagedMenu.legacy("&cOne of the numeric arguments wasn't a valid number."));
        }
        return true;
    }

    private void create(CommandSender sender, String[] args) {
        if (args.length < 8) {
            sender.sendMessage(PagedMenu.legacy("&cUsage: /stockadmin create <symbol> <name> <sector> <startPrice> <sharesOutstanding> <drift> <volatility>"));
            sender.sendMessage(PagedMenu.legacy("&7drift/volatility are fractions per tick, e.g. drift 0.0002 = 0.02%, volatility 0.01 = 1% typical swing."));
            return;
        }
        String symbol = args[1];
        String name = args[2];
        String sector = args[3];
        double startPrice = Double.parseDouble(args[4]);
        long sharesOutstanding = Long.parseLong(args[5]);
        double drift = Double.parseDouble(args[6]);
        double volatility = Double.parseDouble(args[7]);

        String error = adminService.createStock(symbol, name, sector, startPrice, sharesOutstanding, drift, volatility);
        if (error == null) {
            registry.saveAll();
        }
        sender.sendMessage(PagedMenu.legacy(error == null ? "&aListed " + symbol.toUpperCase() + "." : "&c" + error));
    }

    private void delist(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(PagedMenu.legacy("&cUsage: /stockadmin delist <symbol> [payout|nopayout]"));
            return;
        }
        boolean payout = args.length < 3 || !args[2].equalsIgnoreCase("nopayout");
        String error = adminService.delist(args[1], payout);
        if (error == null) {
            registry.saveAll();
        }
        sender.sendMessage(PagedMenu.legacy(error == null ? "&aDelisted " + args[1].toUpperCase() + "." : "&c" + error));
    }

    private void split(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(PagedMenu.legacy("&cUsage: /stockadmin split <symbol> <ratio>"));
            return;
        }
        double ratio = Double.parseDouble(args[2]);
        String error = adminService.split(args[1], ratio);
        if (error == null) {
            registry.saveAll();
        }
        sender.sendMessage(PagedMenu.legacy(error == null ? "&aSplit " + args[1].toUpperCase() + " " + ratio + ":1." : "&c" + error));
    }

    private void halt(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(PagedMenu.legacy("&cUsage: /stockadmin halt <symbol> <durationSeconds>"));
            return;
        }
        long duration = Long.parseLong(args[2]);
        String error = adminService.halt(args[1], duration);
        sender.sendMessage(PagedMenu.legacy(error == null ? "&aHalted " + args[1].toUpperCase() + " for " + duration + "s." : "&c" + error));
    }

    private void resume(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(PagedMenu.legacy("&cUsage: /stockadmin resume <symbol>"));
            return;
        }
        String error = adminService.resume(args[1]);
        sender.sendMessage(PagedMenu.legacy(error == null ? "&aResumed " + args[1].toUpperCase() + "." : "&c" + error));
    }

    private void event(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(PagedMenu.legacy("&cUsage: /stockadmin event <symbol> <driftBoost> <volatilityBoost> <durationSeconds>"));
            return;
        }
        double driftBoost = Double.parseDouble(args[2]);
        double volatilityBoost = Double.parseDouble(args[3]);
        long duration = Long.parseLong(args[4]);
        String error = adminService.triggerEvent(args[1], driftBoost, volatilityBoost, duration);
        sender.sendMessage(PagedMenu.legacy(error == null ? "&aApplied event to " + args[1].toUpperCase() + " for " + duration + "s." : "&c" + error));
    }
}
