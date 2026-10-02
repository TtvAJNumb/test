package com.donututils.donutrep;

import com.donututils.donutrep.economy.EconomyManager;
import com.donututils.donutrep.economy.ShardManager;

import com.donututils.donutrep.market.command.ShardsCommand;
import com.donututils.donutrep.market.command.ShopEditCommand;
import com.donututils.donutrep.market.command.ShopCommand;
import com.donututils.donutrep.market.config.Currency;
import com.donututils.donutrep.market.config.MarketConfig;
import com.donututils.donutrep.market.config.ShopCategory;
import com.donututils.donutrep.market.config.ShopItem;
import com.donututils.donutrep.market.config.ShopMenu;
import com.donututils.donutrep.market.gui.MarketGuiService;
import com.donututils.donutrep.market.gui.MarketMenuClickListener;
import com.donututils.donutrep.market.service.MarketService;

import com.donututils.donutrep.sell.SellLedger;
import com.donututils.donutrep.sell.SellService;
import com.donututils.donutrep.sell.WorthStore;
import com.donututils.donutrep.sell.command.SellAllCommand;
import com.donututils.donutrep.sell.command.SellCommand;
import com.donututils.donutrep.sell.command.SellHandCommand;
import com.donututils.donutrep.sell.command.SellHistoryCommand;
import com.donututils.donutrep.sell.command.SellMultiCommand;
import com.donututils.donutrep.sell.command.SellMultiplierCommand;
import com.donututils.donutrep.sell.command.SellProgressCommand;
import com.donututils.donutrep.sell.command.TopSellCommand;
import com.donututils.donutrep.sell.command.WorthCommand;

import com.donututils.donutrep.auctionhouse.AuctionHouseCommand;
import com.donututils.donutrep.auctionhouse.AuctionHouseManager;

import com.donututils.donutrep.social.ChatCommand;
import com.donututils.donutrep.social.ChatListener;
import com.donututils.donutrep.social.GlobalChatManager;
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
 * DonutREP: DonutREP's own native Vault economy (Money + Shards), a /shop catalog matching real
 * UltimateDonutSmp's shop.yml, a real /sell family priced from real UDS worth.yml, a real
 * player-to-player Auction House and Orders board, social (chat/msg/ignore), homes/warps, AFK,
 * random-teleport, dueling, plus the 7 addon jars this project absorbed (Teams, Crates, staff tools,
 * EconomyWatchdog, MarketWatch, PunishmentHistoryGUI, PurchaseAlert) and an admin/player /help. It
 * exposes the narrow command surface (addshards/removeshards/crate set) that StoreBridge's storefront
 * needs, registered under DonutREP's own name rather than "ultimatedonutsmp:" - edit that prefix out of
 * StoreBridge's config.yml to match.
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

    // ── Sell (/sell family, priced from worth.yml) ──────────────────────────
    private com.donututils.donutrep.sell.db.DatabaseManager sellDatabase;
    private WorthStore worthStore;
    private SellLedger sellLedger;
    private SellService sellService;
    private volatile double sellMultiplier = 1.0;

    // ── Auction House ────────────────────────────────────────────────────────
    private com.donututils.donutrep.auctionhouse.db.DatabaseManager auctionHouseDatabase;
    private AuctionHouseManager auctionHouseManager;

    // ── Orders board ─────────────────────────────────────────────────────────
    private com.donututils.donutrep.orders.db.DatabaseManager ordersDatabase;
    private com.donututils.donutrep.orders.OrderBoardManager orderBoardManager;

    // ── Social ───────────────────────────────────────────────────────────────
    private com.donututils.donutrep.social.db.DatabaseManager socialDatabase;
    private GlobalChatManager globalChatManager;
    private IgnoreManager ignoreManager;
    private MessagingManager messagingManager;

    // ── Homes/Warps ──────────────────────────────────────────────────────────
    private com.donututils.donutrep.homes.db.DatabaseManager homesDatabase;
    private HomeManager homeManager;
    private SpawnManager spawnManager;

    // ── AFK ──────────────────────────────────────────────────────────────────
    private AfkManager afkManager;
    private com.donututils.donutrep.afk.AfkZoneManager afkZoneManager;
    private BukkitTask afkSweepTask;
    private BukkitTask afkKickTask;

    // ── Travel (RTP) ─────────────────────────────────────────────────────────
    private RtpManager rtpManager;
    private RtpQueueService rtpQueueService;
    private volatile RtpConfig rtpConfig;

    // ── Warps / Portals ──────────────────────────────────────────────────────
    private com.donututils.donutrep.warps.db.DatabaseManager warpsDatabase;
    private com.donututils.donutrep.warps.WarpManager warpManager;
    private com.donututils.donutrep.warps.PortalManager portalManager;

    // ── TPA ──────────────────────────────────────────────────────────────────
    private com.donututils.donutrep.tpa.TpaManager tpaManager;

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
    private com.donututils.donutrep.staff.GodManager godManager;
    private com.donututils.donutrep.staff.VanishManager vanishManager;
    private com.donututils.donutrep.staff.StaffModeManager staffModeManager;
    private com.donututils.donutrep.staff.StaffChatManager staffChatManager;

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
        setupAfk();
        setupCrates();
        setupMarket();
        setupSell();
        setupAuctionHouse();
        setupOrders();
        setupSocial();
        setupHomes();
        setupTravel();
        setupWarps();
        setupTpa();
        setupPvp();
        setupTeams();
        setupStaff();
        setupEcoWatch();
        setupMarketWatch();
        setupPunishHistory();
        setupPurchaseAlert();
        registerCommand("help", new com.donututils.donutrep.help.HelpCommand());

        getLogger().info("DonutREP enabled - economy, shop, sell, auction house, social, homes, AFK, "
                + "RTP, duels, Teams, Crates, staff tools, and every watchdog/alert addon are all live.");
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        if (economyDatabase != null) {
            economyDatabase.shutdown();
        }
        if (sellDatabase != null) {
            sellDatabase.shutdown();
        }
        if (auctionHouseDatabase != null) {
            auctionHouseDatabase.shutdown();
        }
        if (ordersDatabase != null) {
            ordersDatabase.shutdown();
        }
        if (socialDatabase != null) {
            socialDatabase.shutdown();
        }
        if (homesDatabase != null) {
            homesDatabase.shutdown();
        }
        cancel(afkSweepTask);
        cancel(afkKickTask);
        if (warpsDatabase != null) {
            warpsDatabase.shutdown();
        }
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
        registerCommand("balance", new com.donututils.donutrep.economy.command.BalanceCommand(economyManager));
        registerCommand("addmoney", new com.donututils.donutrep.economy.command.AddMoneyCommand(economyManager));
        registerCommand("removemoney", new com.donututils.donutrep.economy.command.RemoveMoneyCommand(economyManager));
        registerCommand("setmoney", new com.donututils.donutrep.economy.command.SetMoneyCommand(economyManager));
        registerCommand("shardpay", new com.donututils.donutrep.economy.command.ShardPayCommand(shardManager));
        registerCommand("setshards", new com.donututils.donutrep.economy.command.SetShardsCommand(shardManager));
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
    // MARKET (/shop) - matches real UDS shop.yml exactly: fixed categories/items/
    // prices/slots/display-names/lore, no dynamic pricing, no market-side selling.
    // ═══════════════════════════════════════════════════════════════════════

    private void setupMarket() {
        marketConfig = loadMarketConfig();
        marketService = new MarketService(economyManager, shardManager, crateManager, this::getMarketConfig);
        marketGuiService = new MarketGuiService(marketService, this::getMarketConfig);

        getServer().getPluginManager().registerEvents(new MarketMenuClickListener(), this);

        registerCommand("shop", new ShopCommand(marketGuiService));
        registerCommand("shopedit", new ShopEditCommand(this));
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
                categories.add(new ShopCategory(id, s.getString("display-name", id), icon, s.getInt("slot", 0),
                        s.getStringList("lore")));
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
                        Currency currency;
                        try {
                            currency = Currency.valueOf(is.getString("currency", "MONEY").toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException ex) {
                            currency = Currency.MONEY;
                        }
                        items.add(new ShopItem(
                                itemId,
                                is.getString("display-name", itemId),
                                is.getStringList("lore"),
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
    // SELL (/sell, /sellhand, /sellall, /sellmulti, /sellmultiplier, /sellprogress,
    // /sellhistory, /topsell, /worth) - priced from the bundled worth.yml, entirely
    // separate from /shop's buy catalog.
    // ═══════════════════════════════════════════════════════════════════════

    private void setupSell() {
        sellDatabase = new com.donututils.donutrep.sell.db.DatabaseManager(getDataFolder(), getLogger());
        worthStore = new WorthStore(this);
        sellLedger = new SellLedger(this, sellDatabase);
        sellMultiplier = getConfig().getDouble("sell.multiplier", 1.0);
        sellService = new SellService(worthStore, economyManager, sellLedger, this::getSellMultiplier);

        registerCommand("sell", new SellCommand(sellService));
        registerCommand("sellhand", new SellHandCommand(sellService));
        registerCommand("sellall", new SellAllCommand(sellService));
        registerCommand("sellmulti", new SellMultiCommand(sellService));
        registerCommand("sellmultiplier", new SellMultiplierCommand(this));
        registerCommand("sellprogress", new SellProgressCommand(sellLedger));
        registerCommand("sellhistory", new SellHistoryCommand(sellLedger));
        registerCommand("topsell", new TopSellCommand(sellLedger));
        registerCommand("worth", new WorthCommand(worthStore, sellService));
    }

    public void reloadSell() {
        reloadConfig();
        sellMultiplier = getConfig().getDouble("sell.multiplier", 1.0);
    }

    public double getSellMultiplier() {
        return sellMultiplier;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // AUCTION HOUSE (/auctionhouse, alias /ah - staff force-cancel any listing
    // lives inside /auctionhouse cancel now, gated by auctionhouse.admin, since
    // the real UDS /shopedit name is a different command - shop.yml reload)
    // ═══════════════════════════════════════════════════════════════════════

    private void setupAuctionHouse() {
        auctionHouseDatabase = new com.donututils.donutrep.auctionhouse.db.DatabaseManager(getDataFolder(), getLogger());
        int maxListings = getConfig().getInt("auctionhouse.max-listings-per-player", 10);
        auctionHouseManager = new AuctionHouseManager(this, auctionHouseDatabase, economyManager, maxListings);

        AuctionHouseCommand auctionHouseCommand = new AuctionHouseCommand(auctionHouseManager);
        registerCommand("auctionhouse", auctionHouseCommand);
        registerCommand("ah", auctionHouseCommand);
    }

    public AuctionHouseManager getAuctionHouseManager() {
        return auctionHouseManager;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ORDERS BOARD (/orders)
    // ═══════════════════════════════════════════════════════════════════════

    private void setupOrders() {
        ordersDatabase = new com.donututils.donutrep.orders.db.DatabaseManager(getDataFolder(), getLogger());
        orderBoardManager = new com.donututils.donutrep.orders.OrderBoardManager(this, ordersDatabase, economyManager);

        registerCommand("orders", new com.donututils.donutrep.orders.OrdersCommand(orderBoardManager));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SOCIAL (/chat /msg /reply /pm /ignore /unignore)
    // ═══════════════════════════════════════════════════════════════════════

    private void setupSocial() {
        socialDatabase = new com.donututils.donutrep.social.db.DatabaseManager(getDataFolder(), getLogger());
        globalChatManager = new com.donututils.donutrep.social.GlobalChatManager();
        ignoreManager = new IgnoreManager(this, socialDatabase);
        messagingManager = new MessagingManager(ignoreManager);

        getServer().getPluginManager().registerEvents(new ChatListener(globalChatManager, ignoreManager), this);

        registerCommand("chat", new ChatCommand(globalChatManager));
        MsgCommand msgCommand = new MsgCommand(messagingManager);
        registerCommand("msg", msgCommand);
        registerCommand("pm", msgCommand);
        registerCommand("reply", new ReplyCommand(messagingManager));
        registerCommand("ignore", new IgnoreCommand(ignoreManager));
        registerCommand("unignore", new UnignoreCommand(ignoreManager));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HOMES / SPAWN (/home /homes /sethome /delhome /renamehome /spawn /setspawn)
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

        afkZoneManager = new com.donututils.donutrep.afk.AfkZoneManager(this);
        afkManager.addListener(afkZoneManager);

        long timeoutMillis = getConfig().getInt("afk.timeout-seconds", 300) * 1000L;
        AfkIdleSweeper idleSweeper = new AfkIdleSweeper(afkManager, () -> timeoutMillis);
        afkSweepTask = getServer().getScheduler().runTaskTimer(this, idleSweeper, 20L * 15L, 20L * 15L);

        boolean kickEnabled = getConfig().getBoolean("afk.kick.enabled", false);
        long kickAfterMillis = getConfig().getInt("afk.kick.after-seconds", 1800) * 1000L;
        com.donututils.donutrep.afk.AfkKickSweeper kickSweeper = new com.donututils.donutrep.afk.AfkKickSweeper(
                afkManager, () -> kickEnabled, () -> kickAfterMillis);
        afkKickTask = getServer().getScheduler().runTaskTimer(this, kickSweeper, 20L * 30L, 20L * 30L);

        registerCommand("afk", new AfkCommand(afkManager));
        registerCommand("setafk", new SetAfkCommand(afkManager, afkZoneManager));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TRAVEL (/rtp /rtpq /teleport /randomteleport /findplayer)
    // ═══════════════════════════════════════════════════════════════════════

    private void setupTravel() {
        rtpConfig = loadRtpConfig();
        rtpManager = new RtpManager(this, this::getRtpConfig);
        rtpQueueService = new RtpQueueService(this, rtpManager, this::getRtpConfig);
        rtpQueueService.start();

        registerCommand("rtp", new RtpCommand(rtpManager));
        registerCommand("rtpq", new RtpqCommand(rtpQueueService));
        registerCommand("teleport", new com.donututils.donutrep.travel.TeleportCommand());
        registerCommand("randomteleport", new com.donututils.donutrep.travel.RandomTeleportCommand(rtpManager));
        registerCommand("findplayer", new com.donututils.donutrep.travel.FindPlayerCommand());
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
    // WARPS / PORTALS (/warp /setwarp /delwarp /warpmanager /portalmanager)
    // ═══════════════════════════════════════════════════════════════════════

    private void setupWarps() {
        warpsDatabase = new com.donututils.donutrep.warps.db.DatabaseManager(getDataFolder(), getLogger());
        warpManager = new com.donututils.donutrep.warps.WarpManager(this, warpsDatabase);
        portalManager = new com.donututils.donutrep.warps.PortalManager(this, warpsDatabase);

        getServer().getPluginManager().registerEvents(
                new com.donututils.donutrep.warps.PortalMoveListener(portalManager, warpManager), this);

        registerCommand("warp", new com.donututils.donutrep.warps.WarpCommand(warpManager));
        registerCommand("setwarp", new com.donututils.donutrep.warps.SetWarpCommand(warpManager));
        registerCommand("delwarp", new com.donututils.donutrep.warps.DelWarpCommand(warpManager));
        registerCommand("warpmanager", new com.donututils.donutrep.warps.WarpManagerCommand(warpManager));
        registerCommand("portalmanager", new com.donututils.donutrep.warps.PortalManagerCommand(portalManager));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TPA (/tpa /tpahere /tpaccept /tpadeny /tpacancel /tpauto /tpahereauto)
    // ═══════════════════════════════════════════════════════════════════════

    private void setupTpa() {
        long timeoutMillis = getConfig().getInt("tpa.request-timeout-seconds", 60) * 1000L;
        tpaManager = new com.donututils.donutrep.tpa.TpaManager(timeoutMillis);

        registerCommand("tpa", new com.donututils.donutrep.tpa.TpaCommand(tpaManager));
        registerCommand("tpahere", new com.donututils.donutrep.tpa.TpaHereCommand(tpaManager));
        registerCommand("tpaccept", new com.donututils.donutrep.tpa.TpAcceptCommand(tpaManager));
        registerCommand("tpadeny", new com.donututils.donutrep.tpa.TpaDenyCommand(tpaManager));
        registerCommand("tpacancel", new com.donututils.donutrep.tpa.TpaCancelCommand(tpaManager));
        registerCommand("tpauto", new com.donututils.donutrep.tpa.TpAutoCommand(tpaManager));
        registerCommand("tpahereauto", new com.donututils.donutrep.tpa.TpaHereAutoCommand(tpaManager));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PVP (/duel /queue /leave)
    // ═══════════════════════════════════════════════════════════════════════

    private void setupPvp() {
        long challengeTimeoutMillis = getConfig().getInt("pvp.duel.challenge-timeout-seconds", 30) * 1000L;
        duelManager = new DuelManager(challengeTimeoutMillis);
        duelQueueManager = new DuelQueueManager(duelManager, afkManager);

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

        getServer().getPluginManager().registerEvents(new com.donututils.donutrep.crates.CrateInteractListener(crateManager, afkManager), this);
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
        godManager = new com.donututils.donutrep.staff.GodManager();
        vanishManager = new com.donututils.donutrep.staff.VanishManager(this);
        staffModeManager = new com.donututils.donutrep.staff.StaffModeManager(vanishManager);
        staffChatManager = new com.donututils.donutrep.staff.StaffChatManager();
        com.donututils.donutrep.staff.StaffChatCommand staffChatCommand = new com.donututils.donutrep.staff.StaffChatCommand(staffChatManager);

        getServer().getPluginManager().registerEvents(new com.donututils.donutrep.staff.FreezeListener(freezeManager), this);
        getServer().getPluginManager().registerEvents(new com.donututils.donutrep.staff.gui.StaffMenuClickListener(), this);
        getServer().getPluginManager().registerEvents(new com.donututils.donutrep.staff.GodListener(godManager), this);
        getServer().getPluginManager().registerEvents(new com.donututils.donutrep.staff.VanishJoinListener(vanishManager), this);
        getServer().getPluginManager().registerEvents(new com.donututils.donutrep.staff.StaffChatListener(staffChatManager, staffChatCommand), this);

        registerCommand("sus", new com.donututils.donutrep.staff.SusCommand(staffPanel));
        registerCommand("freeze", new com.donututils.donutrep.staff.FreezeCommand(freezeManager));
        registerCommand("fly", new com.donututils.donutrep.staff.FlyCommand());
        registerCommand("flyspeed", new com.donututils.donutrep.staff.FlySpeedCommand());
        registerCommand("heal", new com.donututils.donutrep.staff.HealCommand());
        registerCommand("feed", new com.donututils.donutrep.staff.FeedCommand());
        com.donututils.donutrep.staff.GamemodeCommand gamemodeCommand = new com.donututils.donutrep.staff.GamemodeCommand();
        registerCommand("gamemode", gamemodeCommand);
        registerCommand("gmc", gamemodeCommand);
        registerCommand("gms", gamemodeCommand);
        registerCommand("gma", gamemodeCommand);
        registerCommand("gmsp", gamemodeCommand);
        com.donututils.donutrep.staff.GodCommand godCommand = new com.donututils.donutrep.staff.GodCommand(godManager);
        registerCommand("god", godCommand);
        registerCommand("godmode", godCommand);
        registerCommand("vanish", new com.donututils.donutrep.staff.VanishCommand(vanishManager));
        registerCommand("invsee", new com.donututils.donutrep.staff.InvseeCommand());
        registerCommand("staffmode", new com.donututils.donutrep.staff.StaffModeCommand(staffModeManager));
        registerCommand("stafflist", new com.donututils.donutrep.staff.StaffListCommand(vanishManager, staffModeManager));
        registerCommand("staffchat", staffChatCommand);
        registerCommand("helpop", new com.donututils.donutrep.staff.HelpOpCommand());
        registerCommand("report", new com.donututils.donutrep.staff.ReportCommand());
        registerCommand("rename", new com.donututils.donutrep.staff.RenameCommand());
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
