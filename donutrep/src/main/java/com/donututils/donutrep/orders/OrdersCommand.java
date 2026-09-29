package com.donututils.donutrep.orders;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

/** /orders - the Orders board, matching real UDS's command name. Real UDS's usage string is just
 * [my|collect|reload], implying order creation happens through a GUI; since this is chat-based, a
 * "create" and "fulfill" subcommand are necessary additions to actually use the board. */
public final class OrdersCommand implements CommandExecutor {

    private final OrderBoardManager orderBoard;

    public OrdersCommand(OrderBoardManager orderBoard) {
        this.orderBoard = orderBoard;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        if (args.length < 1) {
            list(player);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> create(player, args);
            case "fulfill" -> fulfill(player, args);
            case "my" -> my(player);
            case "collect" -> collect(player);
            case "cancel" -> cancel(player, args);
            case "reload" -> player.sendMessage(color("&aOrders board has no reloadable config."));
            default -> list(player);
        }
        return true;
    }

    private void create(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(color("&cUsage: /orders create <price-per-item> <amount> (holding the item you want to buy)"));
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            player.sendMessage(color("&cHold a sample of the item you want to buy first."));
            return;
        }
        double pricePerUnit;
        int amount;
        try {
            pricePerUnit = Double.parseDouble(args[1]);
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            player.sendMessage(color("&cInvalid price or amount."));
            return;
        }
        OrderBoardManager.CreateResult result = orderBoard.create(player, hand.getType(), amount, pricePerUnit);
        switch (result) {
            case SUCCESS -> player.sendMessage(color("&aPosted an order for " + amount + "x " + hand.getType().name()
                    + " at " + formatMoney(pricePerUnit) + "/each (total " + formatMoney(pricePerUnit * amount) + " escrowed)."));
            case INVALID_PRICE -> player.sendMessage(color("&cPrice must be positive."));
            case INVALID_AMOUNT -> player.sendMessage(color("&cAmount must be positive."));
            case INSUFFICIENT_FUNDS -> player.sendMessage(color("&cYou can't afford to escrow that much."));
        }
    }

    private void fulfill(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /orders fulfill <id> (holding the matching items)"));
            return;
        }
        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(color("&cInvalid order id."));
            return;
        }
        OrderBoardManager.FulfillResult result = orderBoard.fulfill(player, id);
        switch (result) {
            case SUCCESS -> player.sendMessage(color("&aOrder #" + id + " fulfilled - payment deposited."));
            case NOT_FOUND -> player.sendMessage(color("&cNo order #" + id + "."));
            case ALREADY_FULFILLED -> player.sendMessage(color("&cSomeone already fulfilled that order."));
            case OWN_ORDER -> player.sendMessage(color("&cYou can't fulfill your own order."));
            case NOT_ENOUGH_ITEMS -> player.sendMessage(color("&cYou don't have enough of the matching item."));
        }
    }

    private void list(Player player) {
        List<BuyOrder> orders = orderBoard.activeOrders();
        if (orders.isEmpty()) {
            player.sendMessage(color("&7The orders board is empty."));
            return;
        }
        player.sendMessage(color("&6&lOrders Board"));
        for (BuyOrder order : orders) {
            player.sendMessage(color("&e#" + order.id() + " &f" + order.amount() + "x " + order.material().name()
                    + " &7wanted by " + order.buyerName() + " &7- " + formatMoney(order.pricePerUnit()) + "/each &8/orders fulfill " + order.id()));
        }
    }

    private void my(Player player) {
        List<BuyOrder> orders = orderBoard.ordersByBuyer(player.getUniqueId());
        if (orders.isEmpty()) {
            player.sendMessage(color("&7You have no orders posted."));
            return;
        }
        player.sendMessage(color("&6&lYour Orders"));
        for (BuyOrder order : orders) {
            String status = order.collected() ? "&7collected" : order.isFulfilled() ? "&afulfilled - /orders collect" : "&ewaiting";
            player.sendMessage(color("&e#" + order.id() + " &f" + order.amount() + "x " + order.material().name()
                    + " &7- " + formatMoney(order.pricePerUnit()) + "/each &7(" + status + "&7)"));
        }
    }

    private void collect(Player player) {
        int delivered = orderBoard.collect(player);
        player.sendMessage(color(delivered > 0 ? "&aCollected " + delivered + " fulfilled order(s)." : "&7Nothing to collect."));
    }

    private void cancel(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /orders cancel <id>"));
            return;
        }
        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(color("&cInvalid order id."));
            return;
        }
        if (orderBoard.cancel(player, id)) {
            player.sendMessage(color("&aOrder #" + id + " cancelled - escrow refunded."));
        } else {
            player.sendMessage(color("&cYou don't have a cancellable order #" + id + "."));
        }
    }

    private static String formatMoney(double amount) {
        return "$" + String.format(Locale.US, "%,.2f", amount);
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
