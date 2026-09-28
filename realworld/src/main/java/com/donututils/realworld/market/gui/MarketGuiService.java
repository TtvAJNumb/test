package com.donututils.realworld.market.gui;

import com.donututils.realworld.arsenal.config.ArsenalConfig;
import com.donututils.realworld.arsenal.config.WeaponDefinition;
import com.donututils.realworld.market.config.BoutiqueItem;
import com.donututils.realworld.market.config.CatalogEntry;
import com.donututils.realworld.market.config.MarketCategory;
import com.donututils.realworld.market.config.MarketConfig;
import com.donututils.realworld.market.pricing.MarketPricingEngine;
import com.donututils.realworld.market.service.MarketService;
import com.donututils.realworld.motors.config.MotorsConfig;
import com.donututils.realworld.motors.config.VehicleDefinition;
import com.donututils.realworld.municipal.config.MunicipalConfig;
import com.donututils.realworld.municipal.config.PermitDefinition;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Builds and opens every Market menu: the root (6 categories + Shard Boutique), each dimension's
 * sub-category list, a paginated item list per sub-category, and the Firearms/Motors/Permits/Boutique
 * storefronts. All navigation uses {@link MarketMenuHolder}/{@link MarketPagedMenu}, so one click
 * listener drives the whole tree. */
public final class MarketGuiService {

    private final List<CatalogEntry> catalog;
    private final MarketPricingEngine pricing;
    private final MarketService marketService;
    private final Supplier<MarketConfig> configSupplier;
    private final Supplier<ArsenalConfig> arsenalConfigSupplier;
    private final Supplier<MotorsConfig> motorsConfigSupplier;
    private final Supplier<MunicipalConfig> municipalConfigSupplier;

    public MarketGuiService(List<CatalogEntry> catalog, MarketPricingEngine pricing, MarketService marketService,
                             Supplier<MarketConfig> configSupplier, Supplier<ArsenalConfig> arsenalConfigSupplier,
                             Supplier<MotorsConfig> motorsConfigSupplier, Supplier<MunicipalConfig> municipalConfigSupplier) {
        this.catalog = catalog;
        this.pricing = pricing;
        this.marketService = marketService;
        this.configSupplier = configSupplier;
        this.arsenalConfigSupplier = arsenalConfigSupplier;
        this.motorsConfigSupplier = motorsConfigSupplier;
        this.municipalConfigSupplier = municipalConfigSupplier;
    }

    public void openRoot(Player player) {
        List<ItemStack> items = new ArrayList<>();
        List<Consumer<Player>> actions = new ArrayList<>();

        items.add(icon(Material.GRASS_BLOCK, "&2&lOverworld Resources", List.of("&7Wood, crops, ore, and mob drops.")));
        actions.add(p -> openDimension(p, MarketCategory.Dimension.OVERWORLD));

        items.add(icon(Material.NETHERRACK, "&4&lNether Dimension", List.of("&7Netherrack, ores, barter goods, fortress drops.")));
        actions.add(p -> openDimension(p, MarketCategory.Dimension.NETHER));

        items.add(icon(Material.END_STONE, "&d&lThe End & Exotic", List.of("&7End stone, chorus, shulker & elytra-adjacent.")));
        actions.add(p -> openDimension(p, MarketCategory.Dimension.END));

        items.add(icon(Material.ARROW, "&c&lFirearms & Ammunition", List.of("&7Arsenal's weapons and ammo, for Money.")));
        actions.add(this::openFirearms);

        items.add(icon(Material.MINECART, "&b&lMotors & Vehicles", List.of("&7Vehicle spawners, for Money.",
                "&7(Fuel/repairs stay on &f/motors refuel&7/&f/motors repair&7.)")));
        actions.add(this::openMotors);

        items.add(icon(Material.PLAYER_HEAD, "&e&lMunicipal & Corporate Permits", List.of("&7Business/building/weapon permits, for Money.")));
        actions.add(this::openPermits);

        items.add(icon(Material.NETHER_STAR, "&5&l✨ Shard Boutique", List.of("&7Cosmetic skins & black-market goods, for Shards.")));
        actions.add(this::openBoutique);

        MarketMenuHolder holder = new MarketMenuHolder();
        MarketPagedMenu.open(player, holder, configSupplier.get().guiTitle(), items, actions, null);
    }

    private void openDimension(Player player, MarketCategory.Dimension dimension) {
        List<ItemStack> items = new ArrayList<>();
        List<Consumer<Player>> actions = new ArrayList<>();
        for (MarketCategory category : MarketCategory.inDimension(dimension)) {
            long count = catalog.stream().filter(e -> e.category() == category).count();
            if (count == 0) {
                continue;
            }
            items.add(icon(Material.CHEST, "&e" + category.displayName(), List.of("&7" + count + " item(s)")));
            actions.add(p -> openSection(p, category));
        }
        MarketMenuHolder holder = new MarketMenuHolder();
        MarketPagedMenu.open(player, holder, dimension.displayName(), items, actions, this::openRoot);
    }

    private void openSection(Player player, MarketCategory category) {
        List<ItemStack> items = new ArrayList<>();
        List<Consumer<Player>> actions = new ArrayList<>();
        List<Consumer<Player>> shiftActions = new ArrayList<>();
        for (CatalogEntry entry : catalog) {
            if (entry.category() != category) {
                continue;
            }
            double buy = pricing.buyPrice(entry);
            double sell = pricing.sellPrice(entry);
            items.add(icon(entry.material(), "&f" + displayName(entry.material()), List.of(
                    String.format(Locale.US, "&aBuy: $%,.2f &7(left-click)", buy),
                    String.format(Locale.US, "&6Sell: $%,.2f/each &7(shift-click, sells your held stack of this)", sell)
            )));
            actions.add(p -> {
                MarketService.TradeResult result = marketService.buy(p, entry, 1);
                p.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
                openSection(p, category);
            });
            shiftActions.add(p -> {
                MarketService.TradeResult result = marketService.sellAllHeld(p, entry.material());
                p.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
                openSection(p, category);
            });
        }
        MarketMenuHolder holder = new MarketMenuHolder();
        MarketPagedMenu.open(player, holder, category.displayName(), items, actions, shiftActions, p -> openDimension(p, category.dimension()));
    }

    private void openFirearms(Player player) {
        List<ItemStack> items = new ArrayList<>();
        List<Consumer<Player>> actions = new ArrayList<>();
        for (WeaponDefinition weapon : arsenalConfigSupplier.get().weapons().values()) {
            items.add(icon(weapon.material(), weapon.displayName(), List.of(
                    String.format(Locale.US, "&aWeapon: $%,.2f &7(left-click)", weapon.priceMoney()),
                    String.format(Locale.US, "&aFull magazine: $%,.2f &7(shift-click)", weapon.ammoPriceMoney())
            )));
            actions.add(p -> {
                MarketService.TradeResult result = marketService.buyWeapon(p, weapon.id());
                p.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
            });
        }
        MarketMenuHolder holder = new MarketMenuHolder();
        MarketPagedMenu.open(player, holder, "&c&lFirearms & Ammunition", items, actions, this::openRoot);
    }

    private void openMotors(Player player) {
        List<ItemStack> items = new ArrayList<>();
        List<Consumer<Player>> actions = new ArrayList<>();
        for (VehicleDefinition vehicle : motorsConfigSupplier.get().vehicles().values()) {
            items.add(icon(vehicle.keyMaterial(), vehicle.displayName(), List.of(
                    String.format(Locale.US, "&aSpawner: $%,.2f", vehicle.priceMoney()),
                    "&7Refuel/repair with &f/motors refuel&7/&f/motors repair&7 after placing.")));
            actions.add(p -> {
                MarketService.TradeResult result = marketService.buyVehicle(p, vehicle.id());
                p.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
            });
        }
        MarketMenuHolder holder = new MarketMenuHolder();
        MarketPagedMenu.open(player, holder, "&b&lMotors & Vehicles", items, actions, this::openRoot);
    }

    private void openPermits(Player player) {
        List<ItemStack> items = new ArrayList<>();
        List<Consumer<Player>> actions = new ArrayList<>();
        for (var entry : municipalConfigSupplier.get().permits().entrySet()) {
            String type = entry.getKey();
            PermitDefinition permit = entry.getValue();
            items.add(icon(Material.PAPER, "&e" + capitalize(type) + " Permit", List.of(
                    String.format(Locale.US, "&aCost: $%,.2f", permit.cost()))));
            actions.add(p -> {
                MarketService.TradeResult result = marketService.buyPermit(p, type);
                p.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
            });
        }
        MarketMenuHolder holder = new MarketMenuHolder();
        MarketPagedMenu.open(player, holder, "&e&lMunicipal & Corporate Permits", items, actions, this::openRoot);
    }

    private void openBoutique(Player player) {
        List<ItemStack> items = new ArrayList<>();
        List<Consumer<Player>> actions = new ArrayList<>();
        for (BoutiqueItem item : configSupplier.get().shardBoutique().values()) {
            items.add(icon(Material.NETHER_STAR, item.displayName(), List.of(
                    "&7" + item.description(),
                    "&d" + item.priceShards() + " Shards")));
            actions.add(p -> {
                MarketService.TradeResult result = marketService.buyBoutiqueItem(p, item);
                p.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
            });
        }
        MarketMenuHolder holder = new MarketMenuHolder();
        MarketPagedMenu.open(player, holder, "&5&l✨ Shard Boutique", items, actions, this::openRoot);
    }

    private static ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            meta.setLore(lore.stream().map(MarketGuiService::color).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String displayName(Material material) {
        String[] words = material.name().toLowerCase(Locale.ROOT).split("_");
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

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
