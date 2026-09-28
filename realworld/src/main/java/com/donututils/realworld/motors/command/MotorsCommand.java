package com.donututils.realworld.motors.command;

import com.donututils.realworld.RealWorldPlugin;
import com.donututils.realworld.motors.config.MotorsConfig;
import com.donututils.realworld.motors.config.VehicleDefinition;
import com.donututils.realworld.motors.economy.VaultEconomyBridge;
import com.donututils.realworld.motors.model.VehicleState;
import com.donututils.realworld.motors.vehicle.VehicleItemFactory;
import com.donututils.realworld.motors.vehicle.VehicleManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public final class MotorsCommand implements CommandExecutor {

    private final RealWorldPlugin plugin;
    private final VehicleManager vehicleManager;
    private final VehicleItemFactory itemFactory;

    public MotorsCommand(RealWorldPlugin plugin, VehicleManager vehicleManager, VehicleItemFactory itemFactory) {
        this.plugin = plugin;
        this.vehicleManager = vehicleManager;
        this.itemFactory = itemFactory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&cUsage: /motors [give <player> <vehicle>|list|info|refuel <amount>|repair|reload]"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> give(sender, args);
            case "list" -> list(sender);
            case "info" -> info(sender);
            case "refuel" -> refuel(sender, args);
            case "repair" -> repair(sender);
            case "reload" -> reload(sender);
            default -> sender.sendMessage(color("&cUnknown /motors subcommand."));
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("motors.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(color("&cUsage: /motors give <player> <vehicle> [amount]"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(color("&c" + args[1] + " isn't online."));
            return;
        }
        VehicleDefinition definition = plugin.getMotorsConfig().vehicle(args[2]);
        if (definition == null) {
            sender.sendMessage(color("&cUnknown vehicle: " + args[2]));
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Integer.parseInt(args[3]));
            } catch (NumberFormatException ex) {
                sender.sendMessage(color("&cInvalid amount."));
                return;
            }
        }
        ItemStack key = itemFactory.createSpawnerKey(definition);
        key.setAmount(amount);
        target.getInventory().addItem(key);
        sender.sendMessage(color("&aGave " + target.getName() + " " + amount + "x " + definition.displayName() + "&a spawner key(s)."));
    }

    private void list(CommandSender sender) {
        if (!sender.hasPermission("motors.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return;
        }
        MotorsConfig config = plugin.getMotorsConfig();
        if (config.vehicles().isEmpty()) {
            sender.sendMessage(color("&7No vehicles configured."));
            return;
        }
        sender.sendMessage(color("&6&lVehicles"));
        for (VehicleDefinition definition : config.vehicles().values()) {
            sender.sendMessage(color("&e" + definition.id() + " &7(" + definition.kind() + ") - " + definition.displayName()));
        }
    }

    private void info(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        VehicleState state = vehicleManager.findRiddenState(player);
        if (state == null) {
            sender.sendMessage(color("&cYou're not riding a Motors vehicle."));
            return;
        }
        VehicleDefinition definition = plugin.getMotorsConfig().vehicle(state.vehicleDefinitionId);
        String name = definition != null ? definition.displayName() : state.vehicleDefinitionId;
        double maxFuel = definition != null ? definition.maxFuel() : state.fuel;
        double maxWear = definition != null ? definition.maxWear() : 100.0;
        sender.sendMessage(color("&6&l" + name));
        sender.sendMessage(color(String.format(Locale.ROOT, "&7Fuel: &f%.1f&7/%.1f", state.fuel, maxFuel)));
        sender.sendMessage(color(String.format(Locale.ROOT, "&7Wear: &f%.1f&7/%.1f", state.wear, maxWear)));
        sender.sendMessage(color(String.format(Locale.ROOT, "&7Mileage: &f%.1f blocks", state.mileage)));
    }

    private void refuel(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /motors refuel <amount>"));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(color("&cInvalid amount."));
            return;
        }
        if (amount <= 0) {
            sender.sendMessage(color("&cAmount must be positive."));
            return;
        }
        VaultEconomyBridge economy = plugin.getEconomy();
        VehicleManager.EconomyOutcome outcome = vehicleManager.refuel(player, amount, economy);
        sender.sendMessage(color(switch (outcome) {
            case SUCCESS -> "&aRefueled your vehicle.";
            case NOT_IN_VEHICLE -> "&cYou're not riding a Motors vehicle.";
            case UNKNOWN_VEHICLE_TYPE -> "&cThat vehicle's type is no longer configured.";
            case INSUFFICIENT_FUNDS -> "&cYou can't afford that much fuel.";
            case FUEL_ALREADY_FULL -> "&cYour tank is already full.";
            case NO_WEAR -> "&cNothing to do.";
        }));
    }

    private void repair(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        VaultEconomyBridge economy = plugin.getEconomy();
        VehicleManager.EconomyOutcome outcome = vehicleManager.repair(player, economy);
        sender.sendMessage(color(switch (outcome) {
            case SUCCESS -> "&aRepaired your vehicle.";
            case NOT_IN_VEHICLE -> "&cYou're not riding a Motors vehicle.";
            case UNKNOWN_VEHICLE_TYPE -> "&cThat vehicle's type is no longer configured.";
            case INSUFFICIENT_FUNDS -> "&cYou can't afford that repair.";
            case NO_WEAR -> "&cYour vehicle doesn't need repair.";
            case FUEL_ALREADY_FULL -> "&cNothing to do.";
        }));
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("motors.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return;
        }
        plugin.reloadMotors();
        sender.sendMessage(color("&aMotors config reloaded."));
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(color("&cOnly players can do that."));
        return null;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
