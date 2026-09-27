package com.donututils.dynamicshop.command;

import com.donututils.dynamicshop.DynamicShopPlugin;
import com.donututils.dynamicshop.config.DynamicShopConfig;
import com.donututils.dynamicshop.currency.CurrencyProvider;
import com.donututils.dynamicshop.engine.PlayerDataRegistry;
import com.donututils.dynamicshop.engine.ShopItemRegistry;
import com.donututils.dynamicshop.model.Loan;
import com.donututils.dynamicshop.model.PlayerShopData;
import com.donututils.dynamicshop.model.ShopItem;
import com.donututils.dynamicshop.service.EconomyStatsService;
import com.donututils.dynamicshop.service.LoanService;
import com.donututils.dynamicshop.service.ShopGuiService;
import com.donututils.dynamicshop.service.TradingService;
import com.donututils.dynamicshop.util.InventoryUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public final class ShopCommand implements CommandExecutor {

    private final DynamicShopPlugin plugin;
    private final ShopGuiService guiService;
    private final ShopItemRegistry itemRegistry;
    private final PlayerDataRegistry playerDataRegistry;
    private final TradingService tradingService;
    private final LoanService loanService;
    private final EconomyStatsService economyStatsService;
    private final Supplier<DynamicShopConfig> configSupplier;

    public ShopCommand(DynamicShopPlugin plugin, ShopGuiService guiService, ShopItemRegistry itemRegistry,
                        PlayerDataRegistry playerDataRegistry, TradingService tradingService,
                        LoanService loanService, EconomyStatsService economyStatsService,
                        Supplier<DynamicShopConfig> configSupplier) {
        this.plugin = plugin;
        this.guiService = guiService;
        this.itemRegistry = itemRegistry;
        this.playerDataRegistry = playerDataRegistry;
        this.tradingService = tradingService;
        this.loanService = loanService;
        this.economyStatsService = economyStatsService;
        this.configSupplier = configSupplier;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("economy")) {
            sendEconomy(sender);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can use the shop."));
            return true;
        }

        if (args.length == 0) {
            maybeSendTutorial(player);
            guiService.open(player, "ALL");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "sell" -> sell(player, args);
            case "sellall" -> sellAll(player);
            case "autosell" -> autosell(player, args);
            case "tutorial" -> sendTutorial(player);
            case "loan" -> loan(player, args);
            case "repay" -> repay(player, args);
            default -> sender.sendMessage(color("&cUsage: /shop | /shop sell hand | /shop sellall | /shop autosell <item> <on|off> | /shop loan <amount> <currency> | /shop repay <amount> | /shop tutorial | /shop economy"));
        }
        return true;
    }

    private void maybeSendTutorial(Player player) {
        if (!configSupplier.get().tutorialFeatureEnabled()) {
            return;
        }
        PlayerShopData data = playerDataRegistry.get(player.getUniqueId());
        if (data.tutorialSeen()) {
            return;
        }
        data.setTutorialSeen(true);
        sendTutorial(player);
    }

    private void sendTutorial(Player player) {
        for (String line : configSupplier.get().tutorialLines()) {
            player.sendMessage(color(line));
        }
    }

    private void sell(Player player, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("hand")) {
            player.sendMessage(color("&cUsage: /shop sell hand"));
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() == Material.AIR || held.getAmount() <= 0) {
            player.sendMessage(color("&cYou aren't holding anything."));
            return;
        }
        String material = held.getType().name();
        ShopItem item = itemRegistry.get(material);
        if (item == null || !item.sellEnabled()) {
            player.sendMessage(color("&cThat isn't sellable here."));
            return;
        }
        long quantity = held.getAmount();
        InventoryUtil.remove(player.getInventory(), held.getType(), quantity);
        TradingService.TradeResult result = tradingService.sell(player.getUniqueId(), material, quantity);
        player.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
    }

    private void sellAll(Player player) {
        Map<Material, Long> counts = new LinkedHashMap<>();
        ItemStack[] contents = player.getInventory().getContents();
        if (contents != null) {
            for (ItemStack stack : contents) {
                if (stack == null || stack.getType() == Material.AIR) {
                    continue;
                }
                counts.merge(stack.getType(), (long) stack.getAmount(), Long::sum);
            }
        }

        int sold = 0;
        for (Map.Entry<Material, Long> entry : counts.entrySet()) {
            String material = entry.getKey().name();
            ShopItem item = itemRegistry.get(material);
            if (item == null || !item.sellEnabled()) {
                continue;
            }
            long owned = InventoryUtil.count(player.getInventory(), entry.getKey());
            if (owned <= 0) {
                continue;
            }
            InventoryUtil.remove(player.getInventory(), entry.getKey(), owned);
            TradingService.TradeResult result = tradingService.sell(player.getUniqueId(), material, owned);
            if (result.success()) {
                sold++;
                player.sendMessage(color("&a" + result.message()));
            } else {
                player.getInventory().addItem(new ItemStack(entry.getKey(), (int) owned));
                player.sendMessage(color("&c" + result.message()));
            }
        }
        if (sold == 0) {
            player.sendMessage(color("&7Nothing in your inventory could be sold here."));
        }
    }

    private void autosell(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(color("&cUsage: /shop autosell <item> <on|off>"));
            return;
        }
        if (!configSupplier.get().autoSellFeatureEnabled()) {
            player.sendMessage(color("&cAuto-sell is disabled on this server."));
            return;
        }
        String material = args[1].toUpperCase(Locale.ROOT);
        if (!itemRegistry.exists(material)) {
            player.sendMessage(color("&cUnknown item: " + material));
            return;
        }
        boolean enable = args[2].equalsIgnoreCase("on");
        PlayerShopData data = playerDataRegistry.get(player.getUniqueId());
        data.setAutoSell(material, enable);
        player.sendMessage(color("&aAuto-sell for " + material + " turned " + (enable ? "&aon" : "&coff") + "&a."));
    }

    private void loan(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(color("&cUsage: /shop loan <amount> <currency>"));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(color("&cInvalid amount."));
            return;
        }
        LoanService.LoanResult result = loanService.borrow(player.getUniqueId(), amount, args[2].toLowerCase(Locale.ROOT));
        player.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
    }

    private void repay(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /shop repay <amount>"));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(color("&cInvalid amount."));
            return;
        }
        LoanService.LoanResult result = loanService.repay(player.getUniqueId(), amount);
        player.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));

        Loan remaining = loanService.get(player.getUniqueId());
        if (remaining != null) {
            player.sendMessage(color("&7Outstanding balance: &f" + String.format(Locale.US, "%,.2f", remaining.principal()) + " " + remaining.currency()));
        }
    }

    private void sendEconomy(CommandSender sender) {
        if (!configSupplier.get().gdpStatsFeatureEnabled()) {
            sender.sendMessage(color("&cEconomy stats are disabled on this server."));
            return;
        }
        EconomyStatsService.EconomyStats stats = economyStatsService.compute();
        sender.sendMessage(color(String.format(Locale.US,
                "&6&lShop Economy &7- last %dh", configSupplier.get().gdpWindowHours())));
        sender.sendMessage(color(String.format(Locale.US, "&7GDP (transaction volume): &f%,.2f &7(%d trades)", stats.gdp(), stats.transactionCount())));
        sender.sendMessage(color(String.format(Locale.US, "&7Total outstanding debt: &f%,.2f", stats.totalDebt())));
        sender.sendMessage(color(String.format(Locale.US, "&7Price index vs launch: %s%+.2f%%",
                stats.inflationPercent() >= 0 ? "&c" : "&a", stats.inflationPercent())));
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
