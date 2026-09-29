package com.donututils.donutrep.sell;

import com.donututils.donutrep.economy.EconomyManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

/** Sell-side logic against real UDS worth prices (worth.yml) - entirely independent of the /shop buy
 * catalog, matching how real UltimateDonutSmp keeps buying and selling as two separate systems. */
public final class SellService {

    public record SellResult(boolean success, String message) {
        static SellResult fail(String message) {
            return new SellResult(false, message);
        }
    }

    private final WorthStore worthStore;
    private final EconomyManager economy;
    private final SellLedger ledger;
    private final DoubleSupplier multiplierSupplier;

    public SellService(WorthStore worthStore, EconomyManager economy, SellLedger ledger, DoubleSupplier multiplierSupplier) {
        this.worthStore = worthStore;
        this.economy = economy;
        this.ledger = ledger;
        this.multiplierSupplier = multiplierSupplier;
    }

    public double unitPrice(Material material) {
        return worthStore.priceFor(material) * multiplierSupplier.getAsDouble();
    }

    public SellResult sellHand(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            return SellResult.fail("You're not holding anything.");
        }
        return sellMaterial(player, hand.getType(), hand.getAmount());
    }

    public SellResult sellMaterial(Player player, Material material, int amount) {
        if (!worthStore.hasPrice(material)) {
            return SellResult.fail("The server doesn't buy " + displayName(material) + ".");
        }
        int held = countHeld(player, material);
        int toSell = Math.min(amount, held);
        if (toSell <= 0) {
            return SellResult.fail("You don't have any " + displayName(material) + ".");
        }
        double total = unitPrice(material) * toSell;
        removeMaterial(player, material, toSell);
        economy.depositPlayer(player, total);
        ledger.record(player, material.name(), toSell, total);
        return new SellResult(true, "Sold " + toSell + "x " + displayName(material) + " for " + formatMoney(total) + ".");
    }

    public SellResult sellAllInventory(Player player) {
        Map<Material, Integer> counts = new HashMap<>();
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            if (worthStore.hasPrice(stack.getType())) {
                counts.merge(stack.getType(), stack.getAmount(), Integer::sum);
            }
        }
        if (counts.isEmpty()) {
            return SellResult.fail("You have nothing sellable.");
        }
        double total = 0;
        int typesSold = 0;
        for (Map.Entry<Material, Integer> held : counts.entrySet()) {
            double price = unitPrice(held.getKey()) * held.getValue();
            removeMaterial(player, held.getKey(), held.getValue());
            ledger.record(player, held.getKey().name(), held.getValue(), price);
            total += price;
            typesSold++;
        }
        economy.depositPlayer(player, total);
        return new SellResult(true, "Sold " + typesSold + " item type(s) for a total of " + formatMoney(total) + ".");
    }

    private int countHeld(Player player, Material material) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private void removeMaterial(Player player, Material material, int amount) {
        ItemStack[] contents = player.getInventory().getContents();
        int remaining = amount;
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != material) {
                continue;
            }
            int take = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            remaining -= take;
            player.getInventory().setItem(i, stack.getAmount() <= 0 ? null : stack);
        }
    }

    public static String displayName(Material material) {
        String[] words = material.name().toLowerCase(java.util.Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }

    public static String formatMoney(double amount) {
        return "$" + String.format(java.util.Locale.US, "%,.2f", amount);
    }
}
