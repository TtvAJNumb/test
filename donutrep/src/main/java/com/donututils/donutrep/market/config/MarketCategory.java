package com.donututils.donutrep.market.config;

/**
 * Every leaf category in the Market GUI's Overworld/Nether/End tree (see the config key note on
 * {@link MarketConfig#categoryMultipliers()}). {@link com.donututils.donutrep.market.catalog.ItemCatalog}
 * assigns every generated catalog entry to exactly one of these by matching the vanilla Material's own
 * name - see that class for the matching rules themselves.
 */
public enum MarketCategory {
    WOOD_AND_FORESTRY("wood-and-forestry", "Wood & Forestry", Dimension.OVERWORLD),
    AGRICULTURE_AND_CROPS("agriculture-and-crops", "Agriculture & Crops", Dimension.OVERWORLD),
    ORES_AND_STONE("ores-and-stone", "Ores & Stone", Dimension.OVERWORLD),
    MOB_DROPS("mob-drops", "Mob Drops", Dimension.OVERWORLD),
    GENERAL_OVERWORLD("general-overworld", "General Goods", Dimension.OVERWORLD),

    NETHERRACK_AND_BASALT("netherrack-and-basalt", "Netherrack & Basalt", Dimension.NETHER),
    NETHER_ORES("nether-ores", "Nether Ores", Dimension.NETHER),
    PIGLIN_BARTER_GOODS("piglin-barter-goods", "Piglin Barter Goods", Dimension.NETHER),
    FORTRESS_DROPS("fortress-drops", "Fortress Drops", Dimension.NETHER),

    END_STONE_AND_PURPUR("end-stone-and-purpur", "End Stone & Purpur", Dimension.END),
    CHORUS_FLORA("chorus-flora", "Chorus Flora", Dimension.END),
    SHULKER_AND_ELYTRA_ADJACENT("shulker-and-elytra-adjacent", "Shulker & Elytra-adjacent", Dimension.END);

    public enum Dimension {
        OVERWORLD("Overworld Resources"),
        NETHER("Nether Dimension"),
        END("The End & Exotic");

        private final String displayName;

        Dimension(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    private final String configKey;
    private final String displayName;
    private final Dimension dimension;

    MarketCategory(String configKey, String displayName, Dimension dimension) {
        this.configKey = configKey;
        this.displayName = displayName;
        this.dimension = dimension;
    }

    public String configKey() {
        return configKey;
    }

    public String displayName() {
        return displayName;
    }

    public Dimension dimension() {
        return dimension;
    }

    public static MarketCategory[] inDimension(Dimension dimension) {
        return java.util.Arrays.stream(values()).filter(c -> c.dimension == dimension).toArray(MarketCategory[]::new);
    }
}
