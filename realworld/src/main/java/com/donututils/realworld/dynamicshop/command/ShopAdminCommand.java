package com.donututils.realworld.dynamicshop.command;

import com.donututils.realworld.RealWorldPlugin;
import com.donututils.realworld.dynamicshop.currency.CurrencyRegistry;
import com.donututils.realworld.dynamicshop.engine.ShopItemRegistry;
import com.donututils.realworld.dynamicshop.model.ShopItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Locale;

/** Service lookups go through the plugin live (e.g. {@code plugin.getItemRegistry()}) rather than a
 * reference captured at construction time, so a /shopadmin reload's rebuilt currency registry
 * actually reaches this command instead of being silently shadowed by a stale one. */
public final class ShopAdminCommand implements CommandExecutor {

    private static final String USAGE = "&cUsage: /shopadmin <additem|removeitem|setprice|setcurrency|setvolatility|setbuyable|setsellable|reload|save> ...";

    private final RealWorldPlugin plugin;

    public ShopAdminCommand(RealWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color(USAGE));
            return true;
        }
        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "additem" -> addItem(sender, args);
                case "removeitem" -> removeItem(sender, args);
                case "setprice" -> setPrice(sender, args);
                case "setcurrency" -> setCurrency(sender, args);
                case "setvolatility" -> setVolatility(sender, args);
                case "setbuyable" -> setFlag(sender, args, true);
                case "setsellable" -> setFlag(sender, args, false);
                case "reload" -> {
                    plugin.reloadDynamicShop();
                    sender.sendMessage(color("&aDynamicShop config reloaded."));
                }
                case "save" -> {
                    plugin.getItemRegistry().saveAll();
                    sender.sendMessage(color("&aShop data saved."));
                }
                default -> sender.sendMessage(color(USAGE));
            }
        } catch (NumberFormatException ex) {
            sender.sendMessage(color("&cOne of the numeric arguments wasn't a valid number."));
        }
        return true;
    }

    private void addItem(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(color("&cUsage: /shopadmin additem <material> <currency> <basePrice> [category]"));
            return;
        }
        ShopItemRegistry itemRegistry = plugin.getItemRegistry();
        CurrencyRegistry currencyRegistry = plugin.getCurrencyRegistry();

        String material = args[1].toUpperCase(Locale.ROOT);
        try {
            Material.valueOf(material);
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(color("&cUnknown material: " + material));
            return;
        }
        if (itemRegistry.exists(material)) {
            sender.sendMessage(color("&c" + material + " is already in the shop."));
            return;
        }
        String currency = args[2].toLowerCase(Locale.ROOT);
        if (!currencyRegistry.isAvailable(currency)) {
            sender.sendMessage(color("&cUnknown or disabled currency: " + currency));
            return;
        }
        double basePrice = Double.parseDouble(args[3]);
        if (basePrice <= 0) {
            sender.sendMessage(color("&cBase price must be positive."));
            return;
        }
        ShopItem item = new ShopItem(material, currency);
        item.setBasePrice(basePrice);
        item.setCurrentPrice(basePrice);
        item.setMinPrice(basePrice * 0.1);
        item.setMaxPrice(basePrice * 10);
        if (args.length >= 5) {
            item.setCategory(args[4]);
        }
        itemRegistry.add(item);
        itemRegistry.saveAll();
        sender.sendMessage(color("&aAdded " + material + " to the shop."));
    }

    private void removeItem(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /shopadmin removeitem <material>"));
            return;
        }
        ShopItemRegistry itemRegistry = plugin.getItemRegistry();
        String material = args[1].toUpperCase(Locale.ROOT);
        if (!itemRegistry.exists(material)) {
            sender.sendMessage(color("&cUnknown shop item: " + material));
            return;
        }
        itemRegistry.remove(material);
        itemRegistry.saveAll();
        sender.sendMessage(color("&aRemoved " + material + " from the shop."));
    }

    private void setPrice(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(color("&cUsage: /shopadmin setprice <material> <price>"));
            return;
        }
        ShopItem item = requireItem(sender, args[1]);
        if (item == null) {
            return;
        }
        double price = Double.parseDouble(args[2]);
        item.setCurrentPrice(price);
        plugin.getItemRegistry().saveAll();
        sender.sendMessage(color("&aSet " + item.material() + "'s price to " + price + "."));
    }

    private void setCurrency(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(color("&cUsage: /shopadmin setcurrency <material> <currency>"));
            return;
        }
        ShopItem item = requireItem(sender, args[1]);
        if (item == null) {
            return;
        }
        String currency = args[2].toLowerCase(Locale.ROOT);
        if (!plugin.getCurrencyRegistry().isAvailable(currency)) {
            sender.sendMessage(color("&cUnknown or disabled currency: " + currency));
            return;
        }
        item.setCurrency(currency);
        plugin.getItemRegistry().saveAll();
        sender.sendMessage(color("&aSet " + item.material() + "'s currency to " + currency + "."));
    }

    private void setVolatility(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(color("&cUsage: /shopadmin setvolatility <material> <volatility>"));
            return;
        }
        ShopItem item = requireItem(sender, args[1]);
        if (item == null) {
            return;
        }
        double volatility = Double.parseDouble(args[2]);
        item.setVolatility(volatility);
        plugin.getItemRegistry().saveAll();
        sender.sendMessage(color("&aSet " + item.material() + "'s volatility to " + volatility + "."));
    }

    private void setFlag(CommandSender sender, String[] args, boolean buyFlag) {
        if (args.length < 3) {
            sender.sendMessage(color("&cUsage: /shopadmin " + (buyFlag ? "setbuyable" : "setsellable") + " <material> <true|false>"));
            return;
        }
        ShopItem item = requireItem(sender, args[1]);
        if (item == null) {
            return;
        }
        boolean value = Boolean.parseBoolean(args[2]);
        if (buyFlag) {
            item.setBuyEnabled(value);
        } else {
            item.setSellEnabled(value);
        }
        plugin.getItemRegistry().saveAll();
        sender.sendMessage(color("&aSet " + item.material() + "'s " + (buyFlag ? "buyable" : "sellable") + " flag to " + value + "."));
    }

    private ShopItem requireItem(CommandSender sender, String material) {
        ShopItem item = plugin.getItemRegistry().get(material);
        if (item == null) {
            sender.sendMessage(color("&cUnknown shop item: " + material.toUpperCase(Locale.ROOT)));
        }
        return item;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
