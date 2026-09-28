package com.donututils.realworld.market.catalog;

import com.donututils.realworld.market.config.CatalogEntry;
import com.donututils.realworld.market.config.MarketCategory;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Generates the entire vanilla-item catalog (Overworld/Nether/End) from the server's own
 * {@link Material} list at startup, instead of hand-typing thousands of near-duplicate config lines -
 * see the "market" section's top comment in config.yml. Deliberately matches on each Material's own
 * name (a String) rather than referencing specific Material constants, so this class works correctly
 * against whatever Material list the actual server it's running on provides (adding vanilla items in a
 * future Minecraft version needs no code change here - they're picked up automatically next restart).
 */
public final class ItemCatalog {

    private ItemCatalog() {
    }

    public static List<CatalogEntry> generate() {
        List<CatalogEntry> entries = new ArrayList<>();
        for (Material material : Material.values()) {
            if (isExcluded(material)) {
                continue;
            }
            String name = material.name();
            MarketCategory category = categorize(name);
            if (category == null) {
                continue;
            }
            entries.add(new CatalogEntry(material, category, rarityTier(name)));
        }
        return entries;
    }

    /** Blocks/items that exist in the Material enum but aren't things a player should ever buy or
     * sell - creative/administrative blocks, portal/void placeholders, and non-item entries. */
    private static boolean isExcluded(Material material) {
        if (!isUsableConstant(material)) {
            return true;
        }
        String n = material.name();
        if (n.equals("AIR") || n.equals("CAVE_AIR") || n.equals("VOID_AIR")) {
            return true;
        }
        String[] blocked = {
                "BARRIER", "STRUCTURE_", "JIGSAW", "DEBUG_STICK", "KNOWLEDGE_BOOK", "COMMAND_BLOCK",
                "COMMAND_MINECART", "SPAWNER", "BEDROCK", "END_PORTAL", "END_GATEWAY", "NETHER_PORTAL",
                "MOVING_PISTON", "PETRIFIED", "LIGHT", "REINFORCED_DEEPSLATE", "FROSTED_ICE",
        };
        for (String b : blocked) {
            if (n.contains(b)) {
                return true;
            }
        }
        return false;
    }

    /** Guards against enum constants that aren't ordinary obtainable materials (some Material values,
     * like internal legacy aliases on older API surfaces, aren't real items) - a cheap try/catch using
     * only methods present on every Bukkit version, so this stays safe even against a stub/partial
     * Material list in a test environment. */
    private static boolean isUsableConstant(Material material) {
        return material != null && material.name() != null && !material.name().isBlank();
    }

    private static MarketCategory categorize(String n) {
        // ── Nether (checked before Overworld's generic buckets, since some nether items would
        // otherwise also match an Overworld ore/stone/wood pattern) ──────────────────────────────
        if (matchesAny(n, "BLAZE", "WITHER_SKELETON", "NETHER_WART", "NETHER_BRICK", "MAGMA_CREAM")) {
            return MarketCategory.FORTRESS_DROPS;
        }
        if (matchesAny(n, "NETHER_QUARTZ_ORE", "NETHERITE_SCRAP", "NETHERITE_", "ANCIENT_DEBRIS", "QUARTZ")) {
            return MarketCategory.NETHER_ORES;
        }
        if (matchesAny(n, "CRYING_OBSIDIAN", "GLOWSTONE", "GRAVEL", "SPECTRAL_ARROW")) {
            return MarketCategory.PIGLIN_BARTER_GOODS;
        }
        if (matchesAny(n, "NETHERRACK", "BASALT", "BLACKSTONE", "SOUL_SAND", "SOUL_SOIL", "MAGMA_BLOCK",
                "CRIMSON", "WARPED", "NETHER_GOLD_ORE", "NETHER_SPROUTS", "WEEPING_VINES", "TWISTING_VINES",
                "SHROOMLIGHT", "OBSIDIAN")) {
            return MarketCategory.NETHERRACK_AND_BASALT;
        }

        // ── The End ──────────────────────────────────────────────────────────────────────────────
        if (matchesAny(n, "SHULKER", "ELYTRA", "DRAGON_BREATH", "DRAGON_EGG", "END_CRYSTAL", "DRAGON_HEAD")) {
            return MarketCategory.SHULKER_AND_ELYTRA_ADJACENT;
        }
        if (matchesAny(n, "CHORUS")) {
            return MarketCategory.CHORUS_FLORA;
        }
        if (matchesAny(n, "END_STONE", "PURPUR", "END_ROD", "ENDER_")) {
            return MarketCategory.END_STONE_AND_PURPUR;
        }

        // ── Overworld ────────────────────────────────────────────────────────────────────────────
        if (n.endsWith("_ORE") || matchesAny(n, "RAW_IRON", "RAW_GOLD", "RAW_COPPER", "IRON_INGOT",
                "GOLD_INGOT", "COPPER_INGOT", "IRON_BLOCK", "GOLD_BLOCK", "COPPER_BLOCK", "DIAMOND",
                "EMERALD", "LAPIS_", "REDSTONE", "COAL", "FLINT", "AMETHYST")) {
            return MarketCategory.ORES_AND_STONE;
        }
        if (matchesAny(n, "STONE", "COBBLESTONE", "DEEPSLATE", "GRANITE", "DIORITE", "ANDESITE", "TUFF",
                "CALCITE", "DRIPSTONE", "CLAY", "SAND", "GRAVEL", "DIRT", "GRASS_BLOCK", "MUD", "SNOW",
                "ICE")) {
            return MarketCategory.ORES_AND_STONE;
        }
        if (matchesAny(n, "LOG", "WOOD", "PLANKS", "SAPLING", "LEAVES", "STICK", "STRIPPED_")) {
            return MarketCategory.WOOD_AND_FORESTRY;
        }
        if (matchesAny(n, "WHEAT", "CARROT", "POTATO", "BEETROOT", "SEEDS", "MELON", "PUMPKIN",
                "SUGAR_CANE", "SUGAR", "BREAD", "HAY_BLOCK", "COCOA", "BAMBOO", "KELP", "NETHER_WART_",
                "SWEET_BERR", "GLOW_BERR", "MUSHROOM", "VINE", "FERN", "GRASS", "FLOWER", "DYE", "POPPY",
                "DANDELION", "TULIP", "ORCHID", "LILY", "SUNFLOWER", "PEONY", "LILAC", "CACTUS",
                "APPLE", "CROP")) {
            return MarketCategory.AGRICULTURE_AND_CROPS;
        }
        if (matchesAny(n, "ROTTEN_FLESH", "BONE", "STRING", "SPIDER_EYE", "GUNPOWDER", "ENDER_PEARL",
                "SLIME_BALL", "LEATHER", "FEATHER", "RABBIT", "MUTTON", "BEEF", "PORKCHOP", "CHICKEN",
                "COD", "SALMON", "TROPICAL_FISH", "PUFFERFISH", "INK_SAC", "GLOW_INK_SAC", "PHANTOM",
                "TURTLE", "SCUTE", "HONEYCOMB", "HONEY_", "WOOL", "EGG", "MILK", "NAUTILUS", "PRISMARINE",
                "SPONGE", "ARMADILLO")) {
            return MarketCategory.MOB_DROPS;
        }

        return MarketCategory.GENERAL_OVERWORLD;
    }

    private static int rarityTier(String n) {
        if (matchesAny(n, "NETHERITE", "ANCIENT_DEBRIS", "DRAGON", "ELYTRA", "TOTEM", "BEACON",
                "NETHER_STAR", "END_CRYSTAL")) {
            return 5;
        }
        if (matchesAny(n, "DIAMOND", "EMERALD", "SHULKER", "ENCHANT")) {
            return 4;
        }
        if (matchesAny(n, "GOLD", "LAPIS", "QUARTZ", "REDSTONE", "AMETHYST", "COPPER", "OBSIDIAN",
                "BLAZE", "GHAST", "CHORUS", "PRISMARINE", "TRIDENT")) {
            return 3;
        }
        if (matchesAny(n, "IRON", "COAL", "LEATHER", "WOOL", "STRING", "BONE", "GUNPOWDER", "SLIME",
                "HONEYCOMB", "ENDER_PEARL")) {
            return 2;
        }
        return 1;
    }

    private static boolean matchesAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    /** Money price per rarity tier (1-5), before the category multiplier from config is applied. */
    public static double basePriceForTier(int tier) {
        return switch (tier) {
            case 5 -> 300.0;
            case 4 -> 75.0;
            case 3 -> 20.0;
            case 2 -> 5.0;
            default -> 1.0;
        };
    }
}
