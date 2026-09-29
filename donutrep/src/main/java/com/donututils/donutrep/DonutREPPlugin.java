package com.donututils.donutrep;

import com.donututils.donutrep.economy.EconomyManager;
import com.donututils.donutrep.economy.ShardManager;

import com.donututils.donutrep.market.command.ShardsCommand;
import com.donututils.donutrep.market.command.ShopAdminCommand;
import com.donututils.donutrep.market.command.ShopCommand;
import com.donututils.donutrep.market.config.BoutiqueItem;
import com.donututils.donutrep.market.config.CatalogEntry;
import com.donututils.donutrep.market.config.MarketConfig;
import com.donututils.donutrep.market.catalog.ItemCatalog;
import com.donututils.donutrep.market.gui.MarketGuiService;
import com.donututils.donutrep.market.gui.MarketMenuClickListener;
import com.donututils.donutrep.market.pricing.MarketPriceStore;
import com.donututils.donutrep.market.pricing.MarketPricingEngine;
import com.donututils.donutrep.market.service.MarketService;

import com.donututils.donutrep.auctionhouse.AuctionHouseManager;
import com.donututils.donutrep.auctionhouse.OrdersCommand;
import com.donututils.donutrep.auctionhouse.ShopEditCommand;

import com.donututils.donutrep.social.ChatCommand;
import com.donututils.donutrep.social.ChatListener;
import com.donututils.donutrep.social.ChatManager;
import com.donututils.donutrep.social.IgnoreCommand;
import com.donututils.donutrep.social.IgnoreManager;
import com.donututils.donutrep.social.MessagingManager;
import com.donututils.donutrep.social.MsgCommand;
import com.donututils.donutrep.social.ReplyCommand;
import com.donututils.donutrep.social.UnignoreCommand;

import com.donututils.donutrep.homes.DelHomeCommand;
import com.donututils.donutrep.homes.HomeCommand;
import com.donututils.donutrep.homes.HomeManager;
import com.donututils.donutrep.homes.HomesCommand;
import com.donututils.donutrep.homes.RenameHomeCommand;
import com.donututils.donutrep.homes.SetHomeCommand;
import com.donututils.donutrep.homes.SetSpawnCommand;
import com.donututils.donutrep.homes.SpawnCommand;
import com.donututils.donutrep.homes.SpawnManager;

import com.donututils.donutrep.afk.AfkActivityListener;
import com.donututils.donutrep.afk.AfkCommand;
import com.donututils.donutrep.afk.AfkIdleSweeper;
import com.donututils.donutrep.afk.AfkManager;
import com.donututils.donutrep.afk.SetAfkCommand;

import com.donututils.donutrep.travel.RtpCommand;
import com.donututils.donutrep.travel.RtpConfig;
import com.donututils.donutrep.travel.RtpManager;
import com.donututils.donutrep.travel.RtpQueueService;
import com.donututils.donutrep.travel.RtpqCommand;

import com.donututils.donutrep.pvp.DuelCommand;
import com.donututils.donutrep.pvp.DuelDeathListener;
import com.donututils.donutrep.pvp.DuelManager;
import com.donututils.donutrep.pvp.DuelQueueManager;
import com.donututils.donutrep.pvp.LeaveCommand;
import com.donututils.donutrep.pvp.QueueCommand;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
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
 * DonutREP: an original SMP economy/society plugin combining a native Vault economy, a dual-currency
 * (Money/Shards) item shop, a real player-to-player auction house, social (chat/msg/ignore),
 * homes/warps, AFK, random-teleport, and dueling - plus the 7 addon jars this project absorbed
 * (Teams, Crates, staff tools, EconomyWatchdog, MarketWatch, PunishmentHistoryGUI, PurchaseAlert) and
 * an admin/player /help. It exposes the narrow command surface (addshards/removeshards/crate set)
 * that StoreBridge's storefront needs, registered under DonutREP's own name rather than
 * "ultimatedonutsmp:" - edit that prefix out of StoreBridge's config.yml to match.
 */
public final class DonutREPPlugin extends JavaPlugin {

    // ── Economy ──────────────────────────────────────────────────────────────
    private com.donututils.donutrep.economy.db.DatabaseManager economyDatabase;
    private EconomyManager economyManager;
    private ShardManager shardManager;

    // ── Market ───────────────────────────────────────────────────────────────
    private List<CatalogEntry> marketCatalog;
    private MarketPriceStore marketPriceStore;
    private MarketPricingEngine marketPricingEngine;
    private MarketService marketService;
    private MarketGuiService marketGuiService;
    private volatile MarketConfig marketConfig;
    private BukkitTask marketPriceTickTask;
    private BukkitTask marketAutosaveTask;

    // ── Auction House ────────────────────────────────────────────────────────
    private com.donututils.donutrep.auctionhouse.db.DatabaseManager auctionHouseDatabase;
    private AuctionHouseManager auctionHouseManager;

    // ── Social ───────────────────────────────────────────────────────────────
    private com.donututils.donutrep.social.db.DatabaseManager socialDatabase;
    private ChatManager chatManager;
    private IgnoreManager ignoreManager;
    private MessagingManager messagingManager;

    // ── Homes/Warps ──────────────────────────────────────────────────────────
    private com.donututils.donutrep.homes.db.DatabaseManager homesDatabase;
    private HomeManager homeManager;
    private SpawnManager spawnManager;

    // ── AFK ──────────────────────────────────────────────────────────────────
    private AfkManager afkManager;
    private BukkitTask afkSweepTask;

    // ── Travel (RTP) ─────────────────────────────────────────────────────────
    private RtpManager rtpManager;
    private RtpQueueService rtpQueueService;
    private volatile RtpConfig rtpConfig;

    // ── PvP (duels) ──────────────────────────────────────────────────────────
    private DuelManager duelManager;
    private DuelQueueManager duelQueueManager;

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
        setupMarket();
        setupAuctionHouse();
        setupSocial();
        setupHomes();
        setupAfk();
        setupTravel();
        setupPvp();
        setupTeams();
        setupCrates();
        setupStaff();
        setupEcoWatch();
        setupMarketWatch();
        setupPunishHistory();
        setupPurchaseAlert();
        registerCommand("help", new com.donututils.donutrep.help.HelpCommand());

        getLogger().info("DonutREP enabled - economy, market, auction house, social, homes, AFK, RTP, "
                + "duels, Teams, Crates, staff tools, and every watchdog/alert addon are all live.");
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        if (economyDatabase != null) {
            economyDatabase.shutdown();
        }

        cancel(marketPriceTickTask);
        cancel(marketAutosaveTask);
        if (marketPricingEngine != null) {
            marketPricingEngine.saveAll();
        }

        if (auctionHouseDatabase != null) {
            auctionHouseDatabase.shutdown();
        }
        if (socialDatabase != null) {
            socialDatabase.shutdown();
        }
        if (homesDatabase != null) {
            homesDatabase.shutdown();
        }
        cancel(afkSweepTask);
        if (rtpQueueService != null) {
            rtpQueueService.stop();
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
    // MARKET
    // ═══════════════════════════════════════════════════════════════════════

    private void setupMarket() {
        marketConfig = loadMarketConfig();
        marketCatalog = ItemCatalog.generate();
        marketPriceStore = new MarketPriceStore(getDataFolder(), getLogger());
        marketPricingEngine = new MarketPricingEngine(marketPriceStore, this::getMarketConfig);
        marketService = new MarketService(marketCatalog, marketPricingEngine, economyManager, shardManager, this::getMarketConfig);
        marketGuiService = new MarketGuiService(marketCatalog, marketPricingEngine, marketService, this::getMarketConfig);

        getServer().getPluginManager().registerEvents(new MarketMenuClickListener(), this);

        registerCommand("shop", new ShopCommand(marketGuiService, marketService));
        registerCommand("shopadmin", new ShopAdminCommand(this, shardManager));

        startMarketTasks();
        getLogger().info("Market enabled with " + marketCatalog.size() + " catalog item(s) generated.");
    }

    public void reloadMarket() {
        reloadConfig();
        marketConfig = loadMarketConfig();
        stopMarketTasks();
        startMarketTasks();
    }

    public MarketConfig getMarketConfig() {
        return marketConfig;
    }

    private void startMarketTasks() {
        long tickTicks = Math.max(20L, marketConfig.priceTickIntervalSeconds() * 20L);
        marketPriceTickTask = getServer().getScheduler().runTaskTimerAsynchronously(this, marketPricingEngine::tick, tickTicks, tickTicks);
        marketAutosaveTask = getServer().getScheduler().runTaskTimer(this, marketPricingEngine::saveAll, 20L * 60L * 5L, 20L * 60L * 5L);
    }

    private void stopMarketTasks() {
        cancel(marketPriceTickTask);
        cancel(marketAutosaveTask);
        marketPriceTickTask = null;
        marketAutosaveTask = null;
    }

    private MarketConfig loadMarketConfig() {
        FileConfiguration cfg = getConfig();

        Map<String, Double> multipliers = new LinkedHashMap<>();
        ConfigurationSection multSection = cfg.getConfigurationSection("market.category-multipliers");
        if (multSection != null) {
            for (String key : multSection.getKeys(false)) {
                multipliers.put(key, multSection.getDouble(key, 1.0));
            }
        }

        Map<String, BoutiqueItem> boutique = new LinkedHashMap<>();
        ConfigurationSection boutiqueSection = cfg.getConfigurationSection("market.shard-boutique");
        if (boutiqueSection != null) {
            for (String id : boutiqueSection.getKeys(false)) {
                ConfigurationSection s = boutiqueSection.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                boutique.put(id, new BoutiqueItem(
                        id,
                        s.getString("display-name", id),
                        s.getString("description", ""),
                        s.getString("material", "STONE"),
                        s.getInt("custom-model-data", 0),
                        s.getLong("price-shards", 100)
                ));
            }
        }

        return new MarketConfig(
                cfg.getString("market.gui.title", "&8Server Market"),
                multipliers,
                cfg.getDouble("market.sell-back-fraction", 0.4),
                cfg.getInt("market.transaction-cooldown-seconds", 1),
                cfg.getInt("market.max-quantity-per-transaction", 6400),
                boutique,
                cfg.getInt("market.dynamic-pricing.tick-interval-seconds", 60),
                cfg.getDouble("market.dynamic-pricing.buy-impact-percent", 0.05),
                cfg.getDouble("market.dynamic-pricing.sell-impact-percent", 0.05),
                cfg.getDouble("market.dynamic-pricing.decay-percent-per-tick", 2.0),
                cfg.getDouble("market.dynamic-pricing.min-price-factor", 0.25),
                cfg.getDouble("market.dynamic-pricing.max-price-factor", 4.0)
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    // AUCTION HOUSE (/orders, /shopedit)
    // ═══════════════════════════════════════════════════════════════════════

    private void setupAuctionHouse() {
        auctionHouseDatabase = new com.donututils.donutrep.auctionhouse.db.DatabaseManager(getDataFolder(), getLogger());
        int maxListings = getConfig().getInt("auctionhouse.max-listings-per-player", 10);
        auctionHouseManager = new AuctionHouseManager(this, auctionHouseDatabase, economyManager, maxListings);

        registerCommand("orders", new OrdersCommand(auctionHouseManager));
        registerCommand("shopedit", new ShopEditCommand(auctionHouseManager));
    }

    public AuctionHouseManager getAuctionHouseManager() {
        return auctionHouseManager;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SOCIAL (/chat /msg /reply /pm /ignore /unignore)
    // ═══════════════════════════════════════════════════════════════════════

    private void setupSocial() {
        socialDatabase = new com.donututils.donutrep.social.db.DatabaseManager(getDataFolder(), getLogger());
        chatManager = new ChatManager();
        ignoreManager = new IgnoreManager(this, socialDatabase);
        messagingManager = new MessagingManager(ignoreManager);

        double localRadius = getConfig().getDouble("social.local-chat-radius-blocks", 100.0);
        getServer().getPluginManager().registerEvents(new ChatListener(chatManager, ignoreManager, () -> localRadius), this);

        registerCommand("chat", new ChatCommand(chatManager));
        MsgCommand msgCommand = new MsgCommand(messagingManager);
        registerCommand("msg", msgCommand);
        registerCommand("pm", msgCommand);
        registerCommand("reply", new ReplyCommand(messagingManager));
        registerCommand("ignore", new IgnoreCommand(ignoreManager));
        registerCommand("unignore", new UnignoreCommand(ignoreManager));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HOMES / WARPS (/home /homes /sethome /delhome /renamehome /spawn /setspawn)
    // ═══════════════════════════════════════════════════════════════════════

    private void setupHomes() {
        homesDatabase = new com.donututils.donutrep.homes.db.DatabaseManager(getDataFolder(), getLogger());
        int maxHomes = getConfig().getInt("homes.max-homes-per-player", 3);
        homeManager = new HomeManager(this, homesDatabase, () -> maxHomes);
        spawnManager = new SpawnManager(this, homesDatabase);

        registerCommand("home", new HomeCommand(homeManager));
        registerCommand("homes", new HomesCommand(homeManager));
        registerCommand("sethome", new SetHomeCommand(homeManager));
        registerCommand("delhome", new DelHomeCommand(homeManager));
        registerCommand("renamehome", new RenameHomeCommand(homeManager));
        registerCommand("spawn", new SpawnCommand(spawnManager));
        registerCommand("setspawn", new SetSpawnCommand(spawnManager));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // AFK (/afk /setafk)
    // ═══════════════════════════════════════════════════════════════════════

    private void setupAfk() {
        afkManager = new AfkManager();
        getServer().getPluginManager().registerEvents(new AfkActivityListener(afkManager), this);

        long timeoutMillis = getConfig().getInt("afk.timeout-seconds", 300) * 1000L;
        AfkIdleSweeper sweeper = new AfkIdleSweeper(afkManager, () -> timeoutMillis);
        afkSweepTask = getServer().getScheduler().runTaskTimer(this, sweeper, 20L * 15L, 20L * 15L);

        registerCommand("afk", new AfkCommand(afkManager));
        registerCommand("setafk", new SetAfkCommand(afkManager));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TRAVEL (/rtp /rtpq)
    // ═══════════════════════════════════════════════════════════════════════

    private void setupTravel() {
        rtpConfig = loadRtpConfig();
        rtpManager = new RtpManager(this, this::getRtpConfig);
        rtpQueueService = new RtpQueueService(this, rtpManager, this::getRtpConfig);
        rtpQueueService.start();

        registerCommand("rtp", new RtpCommand(rtpManager));
        registerCommand("rtpq", new RtpqCommand(rtpQueueService));
    }

    public RtpConfig getRtpConfig() {
        return rtpConfig;
    }

    private RtpConfig loadRtpConfig() {
        FileConfiguration cfg = getConfig();
        return new RtpConfig(
                cfg.getDouble("travel.rtp.min-radius", 200.0),
                cfg.getDouble("travel.rtp.max-radius", 5000.0),
                cfg.getInt("travel.rtp.cooldown-seconds", 30),
                cfg.getInt("travel.rtp.max-attempts", 20),
                cfg.getInt("travel.rtp.max-concurrent-searches", 3),
                cfg.getInt("travel.rtp.queue-interval-seconds", 5)
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PVP (/duel /queue /leave)
    // ═══════════════════════════════════════════════════════════════════════

    private void setupPvp() {
        long challengeTimeoutMillis = getConfig().getInt("pvp.duel.challenge-timeout-seconds", 30) * 1000L;
        duelManager = new DuelManager(challengeTimeoutMillis);
        duelQueueManager = new DuelQueueManager(duelManager);

        getServer().getPluginManager().registerEvents(new DuelDeathListener(duelManager), this);

        registerCommand("duel", new DuelCommand(duelManager));
        registerCommand("queue", new QueueCommand(duelQueueManager));
        registerCommand("leave", new LeaveCommand(duelManager, duelQueueManager));
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
        com.donututils.donutrep.marketwatch.MarketWatchCommand marketWatchCommand =
                new com.donututils.donutrep.marketwatch.MarketWatchCommand(this, marketWatchWebhook, auctionHouseManager);
        registerCommand("marketwatch", marketWatchCommand);
        registerCommand("ahstats", new com.donututils.donutrep.marketwatch.AhStatsCommand(marketWatchCommand));
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
