package com.donututils.donutrep;

import com.donututils.donutrep.economy.EconomyManager;
import com.donututils.donutrep.economy.ShardManager;

import com.donututils.donutrep.market.command.ShardsCommand;
import com.donututils.donutrep.market.command.ShopAdminCommand;
import com.donututils.donutrep.market.command.ShopCommand;
import com.donututils.donutrep.market.config.MarketConfig;
import com.donututils.donutrep.market.config.ShopCategory;
import com.donututils.donutrep.market.config.ShopItem;
import com.donututils.donutrep.market.config.ShopMenu;
import com.donututils.donutrep.market.gui.MarketGuiService;
import com.donututils.donutrep.market.gui.MarketMenuClickListener;
import com.donututils.donutrep.market.service.MarketService;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * DonutREP: DonutREP's own native Vault economy (Money + Shards) plus a /shop catalog matching real
 * UltimateDonutSmp's shop.yml, and the 7 addon jars this project absorbed (Teams, Crates, staff tools,
 * EconomyWatchdog, MarketWatch, PunishmentHistoryGUI, PurchaseAlert) with an admin/player /help.
 * Everything else UDS-shaped (social, homes, AFK, travel, PvP, auction house, orders board) was cut back
 * out on request - this plugin now only covers those 7 addons' original scope plus the economy/shop they
 * all depend on. It exposes the narrow command surface (addshards/removeshards/crate set) that
 * StoreBridge's storefront needs, registered under DonutREP's own name rather than "ultimatedonutsmp:" -
 * edit that prefix out of StoreBridge's config.yml to match.
 */
public final class DonutREPPlugin extends JavaPlugin {

    // ── Economy ──────────────────────────────────────────────────────────────
    private com.donututils.donutrep.economy.db.DatabaseManager economyDatabase;
    private EconomyManager economyManager;
    private ShardManager shardManager;

    // ── Market (/shop) ───────────────────────────────────────────────────────
    private MarketService marketService;
    private MarketGuiService marketGuiService;
    private volatile MarketConfig marketConfig;

    // ── Teams ────────────────────────────────────────────────────────────────
    private com.donututils.donutrep.teams.db.DatabaseManager teamsDatabase;
    private com.donututils.donutrep.teams.TeamManager teamManager;

    // ── Crates ───────────────────────────────────────────────────────────────
    private com.donututils.donutrep.crates.db.DatabaseManager cratesDatabase;
    private com.donututils.donutrep.crates.CrateManager crateManager;
    private volatile com.donututils.donutrep.crates.CrateConfig crateConfig;

    // ── Staff panel ──────────────────────────────────────────────────────────
    private com.donututils.donutrep.staff.FreezeManager freezeManager;

    // ── EconomyWatchdog / MarketWatch / PurchaseAlert (Discord addons) ───────
    private com.donututils.donutrep.ecowatch.discord.DiscordWebhook ecoWatchWebhook;
    private BukkitTask ecoWatchTask;
    private com.donututils.donutrep.marketwatch.discord.DiscordWebhook marketWatchWebhook;
    private com.donututils.donutrep.purchasealert.discord.DiscordWebhook purchaseAlertWebhook;
    private BukkitTask purchaseAlertTask;

    // ── PunishmentHistoryGUI ─────────────────────────────────────────────────
    private com.donututils.donutrep.punishhistory.db.DatabaseManager punishHistoryDatabase;
    private com.donututils.donutrep.punishhistory.NoteStore noteStore;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault is not installed. DonutREP needs Vault (it registers itself as "
                    + "the economy provider, so no separate economy plugin is needed). Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        setupEconomy();
        setupCrates();
        setupMarket();
        setupTeams();
        setupStaff();
        setupEcoWatch();
        setupMarketWatch();
        setupPunishHistory();
        setupPurchaseAlert();
        registerCommand("help", new com.donututils.donutrep.help.HelpCommand());

        getLogger().info("DonutREP enabled - economy, shop, Teams, Crates, staff tools, and every "
                + "watchdog/alert addon are all live.");
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        if (economyDatabase != null) {
            economyDatabase.shutdown();
        }
        if (cratesDatabase != null) {
            cratesDatabase.shutdown();
        }
        if (teamsDatabase != null) {
            teamsDatabase.shutdown();
        }
        if (punishHistoryDatabase != null) {
            punishHistoryDatabase.shutdown();
        }
        cancel(ecoWatchTask);
        cancel(purchaseAlertTask);
    }

    private void cancel(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            getLogger().warning("plugin.yml is missing the '" + name + "' command definition.");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ECONOMY (Money + Shards)
    // ═══════════════════════════════════════════════════════════════════════

    private void setupEconomy() {
        economyDatabase = new com.donututils.donutrep.economy.db.DatabaseManager(getDataFolder(), getLogger());
        economyManager = new EconomyManager(this, economyDatabase, getConfig().getDouble("economy.starting-balance", 1000.0));
        shardManager = new ShardManager(this, economyDatabase);

        getServer().getServicesManager().register(Economy.class, economyManager, this, ServicePriority.Highest);
        getServer().getPluginManager().registerEvents(new EconomyJoinListener(), this);

        registerCommand("pay", new com.donututils.donutrep.economy.command.PayCommand(economyManager));
        registerCommand("addshards", new com.donututils.donutrep.economy.command.AddShardsCommand(shardManager));
        registerCommand("removeshards", new com.donututils.donutrep.economy.command.RemoveShardsCommand(shardManager));
        registerCommand("shards", new ShardsCommand(shardManager));
    }

    private final class EconomyJoinListener implements Listener {
        @EventHandler
        public void onJoin(PlayerJoinEvent event) {
            economyManager.ensureAccount(event.getPlayer().getUniqueId());
        }
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ShardManager getShardManager() {
        return shardManager;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MARKET (/shop) - matches real UDS shop.yml: fixed categories/items/prices,
    // no dynamic pricing, no market-side selling.
    // ═══════════════════════════════════════════════════════════════════════

    private void setupMarket() {
        marketConfig = loadMarketConfig();
        marketService = new MarketService(economyManager, shardManager, crateManager, this::getMarketConfig);
        marketGuiService = new MarketGuiService(marketService, this::getMarketConfig);

        getServer().getPluginManager().registerEvents(new MarketMenuClickListener(), this);

        registerCommand("shop", new ShopCommand(marketGuiService));
        registerCommand("shopadmin", new ShopAdminCommand(this));
    }

    public void reloadMarket() {
        reloadConfig();
        marketConfig = loadMarketConfig();
    }

    public MarketConfig getMarketConfig() {
        return marketConfig;
    }

    private MarketConfig loadMarketConfig() {
        FileConfiguration cfg = getConfig();

        List<ShopCategory> categories = new ArrayList<>();
        ConfigurationSection categoriesSection = cfg.getConfigurationSection("market.categories");
        if (categoriesSection != null) {
            for (String id : categoriesSection.getKeys(false)) {
                ConfigurationSection s = categoriesSection.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                Material icon = materialOrDefault(s.getString("material", "STONE"), Material.STONE);
                categories.add(new ShopCategory(id, s.getString("display-name", id), icon, s.getInt("slot", 0)));
            }
        }

        Map<String, ShopMenu> menus = new LinkedHashMap<>();
        ConfigurationSection menusSection = cfg.getConfigurationSection("market.menus");
        if (menusSection != null) {
            for (String categoryId : menusSection.getKeys(false)) {
                ConfigurationSection menuSection = menusSection.getConfigurationSection(categoryId);
                if (menuSection == null) {
                    continue;
                }
                List<ShopItem> items = new ArrayList<>();
                ConfigurationSection itemsSection = menuSection.getConfigurationSection("items");
                if (itemsSection != null) {
                    for (String itemId : itemsSection.getKeys(false)) {
                        ConfigurationSection is = itemsSection.getConfigurationSection(itemId);
                        if (is == null) {
                            continue;
                        }
                        Material material = materialOrDefault(is.getString("material", "STONE"), Material.STONE);
                        com.donututils.donutrep.market.config.Currency currency;
                        try {
                            currency = com.donututils.donutrep.market.config.Currency.valueOf(
                                    is.getString("currency", "MONEY").toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException ex) {
                            currency = com.donututils.donutrep.market.config.Currency.MONEY;
                        }
                        items.add(new ShopItem(
                                itemId,
                                is.getString("display-name", itemId),
                                material,
                                is.getDouble("price", 0.0),
                                currency,
                                is.getInt("slot", 0),
                                is.getInt("max-quantity", 0),
                                is.getString("crate", null),
                                is.getString("spawner-mob", null)
                        ));
                    }
                }
                menus.put(categoryId, new ShopMenu(menuSection.getString("title", categoryId), items));
            }
        }

        return new MarketConfig(
                cfg.getString("market.gui.title", "&8shop"),
                categories,
                menus,
                cfg.getInt("market.max-quantity-per-transaction", 64)
        );
    }

    private static Material materialOrDefault(String name, Material fallback) {
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TEAMS
    // ═══════════════════════════════════════════════════════════════════════

    private void setupTeams() {
        teamsDatabase = new com.donututils.donutrep.teams.db.DatabaseManager(getDataFolder(), getLogger());
        teamManager = new com.donututils.donutrep.teams.TeamManager(this, teamsDatabase);

        registerCommand("team", new com.donututils.donutrep.teams.TeamCommand(teamManager));
        registerCommand("teambaltop", new com.donututils.donutrep.teams.TeamLeaderboardCommand(
                teamManager, economyManager, shardManager, com.donututils.donutrep.teams.TeamLeaderboardCommand.Stat.MONEY));
        registerCommand("teamshardstop", new com.donututils.donutrep.teams.TeamLeaderboardCommand(
                teamManager, economyManager, shardManager, com.donututils.donutrep.teams.TeamLeaderboardCommand.Stat.SHARDS));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CRATES
    // ═══════════════════════════════════════════════════════════════════════

    private void setupCrates() {
        crateConfig = loadCrateConfig();
        cratesDatabase = new com.donututils.donutrep.crates.db.DatabaseManager(getDataFolder(), getLogger());
        com.donututils.donutrep.crates.CrateKeys crateKeys = new com.donututils.donutrep.crates.CrateKeys(this);
        com.donututils.donutrep.crates.CrateItemFactory crateItemFactory = new com.donututils.donutrep.crates.CrateItemFactory(crateKeys);
        crateManager = new com.donututils.donutrep.crates.CrateManager(this, cratesDatabase, crateKeys, crateItemFactory,
                economyManager, shardManager, this::getCrateConfig);
        crateManager.respawnHolograms();

        getServer().getPluginManager().registerEvents(new com.donututils.donutrep.crates.CrateInteractListener(crateManager), this);
        registerCommand("crate", new com.donututils.donutrep.crates.CrateCommand(this, crateManager));
    }

    public void reloadCrates() {
        reloadConfig();
        crateConfig = loadCrateConfig();
    }

    public com.donututils.donutrep.crates.CrateConfig getCrateConfig() {
        return crateConfig;
    }

    private com.donututils.donutrep.crates.CrateConfig loadCrateConfig() {
        FileConfiguration cfg = getConfig();
        Map<String, com.donututils.donutrep.crates.CrateDefinition> crates = new LinkedHashMap<>();
        ConfigurationSection cratesSection = cfg.getConfigurationSection("crates");
        if (cratesSection != null) {
            for (String id : cratesSection.getKeys(false)) {
                ConfigurationSection s = cratesSection.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                try {
                    org.bukkit.Material keyMaterial = org.bukkit.Material.valueOf(s.getString("key-material", "TRIPWIRE_HOOK").toUpperCase(Locale.ROOT));
                    List<com.donututils.donutrep.crates.CrateReward> rewards = new ArrayList<>();
                    ConfigurationSection rewardsSection = s.getConfigurationSection("rewards");
                    if (rewardsSection != null) {
                        for (String rewardId : rewardsSection.getKeys(false)) {
                            ConfigurationSection r = rewardsSection.getConfigurationSection(rewardId);
                            if (r == null) {
                                continue;
                            }
                            try {
                                com.donututils.donutrep.crates.CrateReward.Kind kind =
                                        com.donututils.donutrep.crates.CrateReward.Kind.valueOf(r.getString("kind", "ITEM").toUpperCase(Locale.ROOT));
                                org.bukkit.Material material = kind == com.donututils.donutrep.crates.CrateReward.Kind.ITEM
                                        ? org.bukkit.Material.valueOf(r.getString("material", "STONE").toUpperCase(Locale.ROOT)) : null;
                                rewards.add(new com.donututils.donutrep.crates.CrateReward(
                                        kind, material,
                                        r.getDouble("money-amount", 0.0),
                                        r.getLong("shards-amount", 0),
                                        r.getInt("item-amount-min", 1),
                                        r.getInt("item-amount-max", 1),
                                        r.getString("display-name", rewardId),
                                        r.getInt("weight", 1)
                                ));
                            } catch (IllegalArgumentException ex) {
                                getLogger().warning("Skipping reward '" + rewardId + "' in crate '" + id + "' - invalid config: " + ex.getMessage());
                            }
                        }
                    }
                    crates.put(id.toLowerCase(Locale.ROOT), new com.donututils.donutrep.crates.CrateDefinition(
                            id, s.getString("display-name", id), keyMaterial, s.getInt("key-custom-model-data", 0), rewards));
                } catch (IllegalArgumentException ex) {
                    getLogger().warning("Skipping crate '" + id + "' - invalid config: " + ex.getMessage());
                }
            }
        }
        return new com.donututils.donutrep.crates.CrateConfig(crates);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STAFF PANEL (/sus)
    // ═══════════════════════════════════════════════════════════════════════

    private void setupStaff() {
        freezeManager = new com.donututils.donutrep.staff.FreezeManager();
        com.donututils.donutrep.staff.StaffPanel staffPanel = new com.donututils.donutrep.staff.StaffPanel(freezeManager);

        getServer().getPluginManager().registerEvents(new com.donututils.donutrep.staff.FreezeListener(freezeManager), this);
        getServer().getPluginManager().registerEvents(new com.donututils.donutrep.staff.gui.StaffMenuClickListener(), this);
        registerCommand("sus", new com.donututils.donutrep.staff.SusCommand(staffPanel));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ECOWATCH
    // ═══════════════════════════════════════════════════════════════════════

    private void setupEcoWatch() {
        ecoWatchWebhook = new com.donututils.donutrep.ecowatch.discord.DiscordWebhook(this);
        startEcoWatchTask();
        registerCommand("ecowatch", new com.donututils.donutrep.ecowatch.EcoWatchCommand(this, ecoWatchWebhook));
    }

    public void reloadEcoWatch() {
        reloadConfig();
        cancel(ecoWatchTask);
        startEcoWatchTask();
    }

    private void startEcoWatchTask() {
        FileConfiguration cfg = getConfig();
        ecoWatchWebhook.configure(cfg.getString("ecowatch.discord.webhook-url", ""), cfg.getString("ecowatch.discord.username", "EconomyWatchdog"));
        double threshold = cfg.getDouble("ecowatch.balance-jump-threshold", 10000.0);
        com.donututils.donutrep.ecowatch.BalanceWatcher watcher =
                new com.donututils.donutrep.ecowatch.BalanceWatcher(economyManager, ecoWatchWebhook, () -> threshold);
        long intervalTicks = Math.max(20L, cfg.getInt("ecowatch.check-interval-seconds", 60) * 20L);
        ecoWatchTask = getServer().getScheduler().runTaskTimerAsynchronously(this, watcher, intervalTicks, intervalTicks);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MARKETWATCH
    // ═══════════════════════════════════════════════════════════════════════

    private void setupMarketWatch() {
        marketWatchWebhook = new com.donututils.donutrep.marketwatch.discord.DiscordWebhook(this);
        marketWatchWebhook.configure(getConfig().getString("marketwatch.discord.webhook-url", ""),
                getConfig().getString("marketwatch.discord.username", "MarketWatch"));
        registerCommand("marketwatch", new com.donututils.donutrep.marketwatch.MarketWatchCommand(this, marketWatchWebhook));
    }

    public void reloadMarketWatch() {
        reloadConfig();
        marketWatchWebhook.configure(getConfig().getString("marketwatch.discord.webhook-url", ""),
                getConfig().getString("marketwatch.discord.username", "MarketWatch"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUNISHMENT HISTORY / STAFF NOTES
    // ═══════════════════════════════════════════════════════════════════════

    private void setupPunishHistory() {
        punishHistoryDatabase = new com.donututils.donutrep.punishhistory.db.DatabaseManager(getDataFolder(), getLogger());
        noteStore = new com.donututils.donutrep.punishhistory.NoteStore(punishHistoryDatabase, getLogger());

        registerCommand("punishhistory", new com.donututils.donutrep.punishhistory.PunishHistoryCommand(noteStore));
        registerCommand("note", new com.donututils.donutrep.punishhistory.NoteCommand(noteStore));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PURCHASE ALERT
    // ═══════════════════════════════════════════════════════════════════════

    private void setupPurchaseAlert() {
        purchaseAlertWebhook = new com.donututils.donutrep.purchasealert.discord.DiscordWebhook(this);
        startPurchaseAlertTask();
        registerCommand("purchasealert", new com.donututils.donutrep.purchasealert.command.PurchaseAlertCommand(this));
    }

    public void reloadPurchaseAlert() {
        reloadConfig();
        cancel(purchaseAlertTask);
        startPurchaseAlertTask();
    }

    public com.donututils.donutrep.purchasealert.discord.DiscordWebhook getPurchaseAlertWebhook() {
        return purchaseAlertWebhook;
    }

    private void startPurchaseAlertTask() {
        FileConfiguration cfg = getConfig();
        purchaseAlertWebhook.configure(cfg.getString("purchasealert.discord.webhook-url", ""),
                cfg.getString("purchasealert.discord.username", "Store Purchases"));

        String backendUrl = cfg.getString("purchasealert.backend_url", "");
        String pluginKey = cfg.getString("purchasealert.plugin_key", "");
        if (backendUrl.isBlank() || pluginKey.isBlank()) {
            getLogger().warning("purchasealert.backend_url or purchasealert.plugin_key is not set - copy the same "
                    + "two values from StoreBridge's own config.yml. Purchase polling is paused until then.");
            return;
        }
        int orderLimit = Math.max(1, Math.min(25, cfg.getInt("purchasealert.order_limit", 10)));
        com.donututils.donutrep.purchasealert.http.BackendOrdersClient client =
                new com.donututils.donutrep.purchasealert.http.BackendOrdersClient(backendUrl, pluginKey);
        com.donututils.donutrep.purchasealert.config.AlertConfig alertConfig = new com.donututils.donutrep.purchasealert.config.AlertConfig(
                backendUrl, pluginKey, Math.max(5, cfg.getInt("purchasealert.poll_interval_seconds", 15)), orderLimit,
                cfg.getString("purchasealert.discord.webhook-url", ""), cfg.getString("purchasealert.discord.username", "Store Purchases"));
        com.donututils.donutrep.purchasealert.watch.PurchaseWatcher watcher =
                new com.donututils.donutrep.purchasealert.watch.PurchaseWatcher(this, client, purchaseAlertWebhook, alertConfig);
        long intervalTicks = Math.max(20L, alertConfig.pollIntervalSeconds() * 20L);
        purchaseAlertTask = getServer().getScheduler().runTaskTimerAsynchronously(this, watcher, intervalTicks, intervalTicks);
    }
}
