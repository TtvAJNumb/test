# DonutSMP staff add-ons

Two Bukkit/Paper plugins built to sit alongside three plugins you already run:
**UltimateDonutSmp** (the SMP core), **CrateBindAddon** (a small addon to it), and **GrimAC**
(anticheat). This file explains how those three existing plugins work, then documents the two
new ones and exactly how each hooks into them.

Both new plugins are independent Maven modules under this repo's root `pom.xml`:

- `punishment-history-gui/` — **PunishmentHistoryGUI**
- `economy-watchdog/` — **EconomyWatchdog**

Built jars are in `dist/` — see **Getting the jars** at the bottom for how they were produced
and verified, since this sandbox can't reach `repo.papermc.io` to run a normal `mvn package`.

---

## 1. How the three plugins you sent actually work

I decompiled all three jars (with CFR) to understand their real internals rather than
guessing from plugin.yml alone. Summary of what's inside each:

### UltimateDonutSmp-1.5.jar

A large (6,700+ class) commercial SMP core plugin, package `com.bx.ultimateDonutSmp`, not
obfuscated — class and method names are intact, which is what made building against it by
reflection practical. It shades its own copies of Gson, Protobuf, the MongoDB driver, and a
Jedis (Redis) client, but the gameplay data itself (economy, punishments, crates, auctions) is
stored through a hand-written `DatabaseManager` using plain JDBC (`Connection`/`PreparedStatement`,
`CREATE TABLE IF NOT EXISTS ...`) — so day-to-day state lives in a local SQL database, not Mongo
(Mongo/Redis are almost certainly there for optional cross-server/network features like the
`NetworkStatusManager`, `RedisManager`, `NetworkStaffChatManager` you can see in its class list).

It's one monolithic plugin with ~90 internal manager classes (`getXManager()` accessors on the
main `UltimateDonutSmp` class) covering: teams, homes/warps, an RTP system, a full shop and
auction house, crates, a shard/spawner economy, duels/FFA, punishments, staff mode, voice chat
consent, a Discord webhook manager, and more. None of this is exposed as a documented public
API — there's no separate "API jar" to compile against — but the classes themselves are public
with public methods, so any other plugin on the same server can call into them via reflection
(or even a direct compile-time dependency on the jar, though UDS's own addon does it via
reflection — see next).

Two things mattered most for the new plugins:

- **Punishments** (`managers/PunishmentManager` + `models/PunishmentRecord`): fully logged —
  bans, mutes, voice-mutes, warns, kicks and blacklists are all persisted with issuer, reason,
  timestamps, expiry and removal metadata, and queryable per-player (`getHistory`) or
  server-wide (`getAll`, paginated, with search). There is **no "note" concept** at all — the
  enum is `BAN, MUTE, VOICE_MUTE, WARN, KICK, BLACKLIST`, nothing else.
- **Economy & auctions** (`managers/EconomyManager`, `managers/AuctionHouseManager`): balances
  support deposit/withdraw/setBalance/transfer, each tagged with an `EconomyReason` enum (shop
  purchase, pay, auction fee, bounty, etc.) — but **that reason is never written anywhere**;
  there is no economy transaction ledger table in `DatabaseManager` at all, only the current
  balance. The auction house *does* keep listings with a `SOLD` status, seller/buyer UUIDs, and
  price, held in an in-memory cache (`AuctionHouseManager`'s private `listingCache` field) that
  the public API only exposes filtered views of (`getActiveListings`, `getPlayerListings`, etc.)
  — there's no public "give me recent sales" method.
- It also has its own `DiscordWebhookManager`, `PunishmentHistoryMenu`/`PunishmentsListMenu`
  GUIs, `CrateManager` (crate definitions/rewards/odds), and `AuctionHouseBrowseMenu` etc. — none
  of that is reused directly by the two plugins below (see the design notes under each for why).

### CrateBindAddon.jar

Tiny — 3 classes, `dev.cratebind`. It has **no storage or state of its own**. On enable it
grabs the running `UltimateDonutSmp` plugin instance and, via `UdsBridge`, resolves five methods
on UDS's `CrateManager` purely by reflection (`getCrate`, `isBindableBlock`, `getBoundCrateId`,
`bindCrateBlock`, `unbindCrateBlock`) plus two on its `CrateVisualManager` for holograms. A
`BindListener` intercepts `/create binditem <crate>` and `/create unbinditem` via
`PlayerCommandPreprocessEvent` at `LOWEST` priority, resolves the block the player is looking at
(`getTargetBlockExact(8)`), and calls straight into UDS. If UDS's shape doesn't match what it
expects, it catches `ReflectiveOperationException` on enable and disables itself with a clear
log message rather than throwing later.

This reflection-bridge pattern (resolve methods once on enable, disable cleanly if they've
moved) is exactly what both new plugins below copy.

### grimac-bukkit-2.3.74-5920e74.jar

This is **GrimAC**, the well-known free/open-source ("libre") anticheat, built on PacketEvents
2.0 (`ac.grim.grimac.platform.bukkit.GrimACBukkitLoaderPlugin` as the Bukkit entry point). Unlike
the other two, it ships a real, deliberately-designed public API package
(`ac.grim.grimac.api.*`): a `GrimPlugin`/extension system, an `EventBus` with typed events
(`FlagEvent`, `GrimUserEvent`, `GrimSetbackEvent`, `GrimReloadEvent`, ...), an `AlertManager`, and
even a full pluggable storage layer (`ac.grim.grimac.api.storage.*` — backends, migrations,
history/violation queries) for building dashboards or bots on top of check history. 135+ check
implementations live under `ac.grim.grimac.checks.impl`, organized by category (combat,
movement, exploit, etc.), each reporting through that event bus. Neither of the two plugins here
needed to touch GrimAC, so nothing integrates with it — but if you ever want a "cheater alert"
bot, it's the one of the three with an actual documented extension API instead of reflection.

---

## 2. PunishmentHistoryGUI

**Commands:** `/punishhistory [player]` (aliases `/phistory`, `/pgui`), `/note <add|remove|list>
<player> [text|index]`.
**Permission for `/punishhistory`:** UDS's own `ultimatedonutsmp.staff.punishments.view` (so
whoever can already see punishments in-game can use this menu too, with no extra config).
**Note permissions:** `punishmenthistorygui.notes.{view,add,remove}` (default op).
**Config file:** `plugins/PunishmentHistoryGUI/config.yml` (menu titles, roster scan size, notes
cap per player) — created automatically on first run, edit and `/punishhistory` again or restart
to apply (no Discord webhook here; that's only in EconomyWatchdog).

### What it shows

- `/punishhistory` with no argument opens a **roster**: every player who shows up in UDS's most
  recent punishment records (scanned up to `roster-scan-size`, default 200, server-wide) or who
  has a staff note on file, sorted by most recent activity, rendered as clickable player heads
  with punishment/note counts in the lore.
- Clicking a head (or running `/punishhistory <player>` directly) opens the **detail page**: that
  player's bans, mutes, voice-mutes, warns, kicks and blacklists from UDS's `PunishmentManager`,
  interleaved chronologically with this plugin's own staff notes, one item per entry — reason,
  issuer, state (active/expired/removed), expiry, and who/why it was removed if applicable.

This is the "roster page → detail page" pattern you described from SusPlayerFinder. That
plugin's jar wasn't attached, so this is a clean, independent implementation of the same idea
(paginated head roster, click through to a paginated detail view, back button returns to the
roster) rather than a reuse of its actual code.

### How it integrates

- All punishment data comes straight from UDS's `PunishmentManager` via reflection
  (`getHistory`, `getAll`, `countAll`, `resolveTargetUuid`, `resolveTargetName`, `getDisplayType`,
  `getState`) — nothing is duplicated or cached long-term, so it's always current and UDS stays
  the single source of truth for punishments.
- **Notes are new** — UDS has no such concept (its `PunishmentType` enum is
  `BAN/MUTE/VOICE_MUTE/WARN/KICK/BLACKLIST` only). They're stored locally, one YAML file per
  player at `plugins/PunishmentHistoryGUI/notes/<uuid>.yml`, capped at `max-notes-per-player`.

---

## 3. EconomyWatchdog

**Command:** `/ecowatch <reload|test|status>` (permission `economywatchdog.admin`).
**Config file:** `plugins/EconomyWatchdog/config.yml` — this is where you paste your Discord
webhook URL (`discord.webhook-url`) and tune every threshold below. `/ecowatch reload` picks up
changes without a restart; `/ecowatch test` sends a sample alert so you can confirm the webhook
is wired up correctly; `/ecowatch status` just tells you whether a webhook URL is set.

### What it does

Posts Discord embeds (via a plain webhook, JDK `HttpClient`, no extra dependency) for:

1. **Suspicious balance jumps** — every `poll-interval-seconds` (default 60), it snapshots every
   *online* player's balance via `EconomyManager.getBalance(uuid)` and compares it to the last
   snapshot. A jump over `balance-jump.min-absolute-delta` **or** `min-percent-delta` (of the
   previous balance) fires an alert with old/new balance and the delta.
2. **Large transfers** — watches `/pay <player> <amount>` as it's typed (same
   `PlayerCommandPreprocessEvent`-at-`MONITOR`-priority pattern CrateBindAddon uses for
   `/create binditem`) and alerts on any attempt at or above `large-transfer.min-amount`.
3. **Odd auction sales** — each poll, reads UDS's auction house cache for newly `SOLD` listings
   and flags sale prices at/above `auction.high-price-flag`, at/below `auction.low-price-flag`
   (penny-dumping items to an alt), and the same buyer/seller pair completing
   `repeat-trading.min-trades`+ trades within `repeat-trading.window-minutes` (a common
   dupe/RMT-laundering pattern between two accounts).

Every one of those three detectors has its own `enabled: true/false` switch in config.yml, so
you can turn any of them off independently.

### Why it's built this way — and its real limits

**UDS fires no events for any of this.** There's no `EconomyTransactionEvent`, no
`AuctionSaleEvent`, and (as noted above) no economy transaction log or "recent sales" table to
query either. So every detector here is either polling public getters
(`EconomyManager.getBalance`) or watching command input the way CrateBindAddon already does —
except one: there's no public method at all for "recently sold auction listings", so
`reflect/UdsBridge.java` reaches one level deeper than PunishmentHistoryGUI does, and reflects a
**private field** (`AuctionHouseManager#listingCache`) that the public `getActiveListings(...)`
method itself filters down from. That's flagged clearly in that class's Javadoc as the one place
in either plugin that isn't just "call a public method reflectively" — it's the most likely
thing to need a small fix if a future UDS update reshapes that class internally, and the plugin
degrades gracefully (logs a warning, skips that poll) rather than crashing if it does.

Because of all that, treat every alert as **advisory, not proof**:

- A large-`/pay` alert fires on the *typed* amount, not a confirmed transfer — a failed (e.g.
  insufficient-funds) `/pay` still alerts.
- A balance-jump alert only ever compares two poll snapshots of *online* players — a deposit and
  withdrawal that both happen within one polling interval will net out and never be seen; shorten
  `poll-interval-seconds` to tighten that at the cost of more HTTP calls to Discord.
- The auction/repeat-trading detector only sees listings currently sitting in memory (recent,
  bounded window) — it isn't a full historical audit.

That's the honest trade-off of bolting a watchdog onto a plugin that wasn't designed to be
watched. It should still catch the obvious cases (a player's balance suddenly 100x'ing, a huge
`/pay` to a brand-new alt, an enchanted netherite sword "sold" for $1) well before staff would
otherwise notice from logs.

### Setting up the Discord webhook

1. In Discord: the target channel's settings → Integrations → Webhooks → New Webhook → copy its
   URL.
2. Drop the jar in `plugins/`, start the server once (so it generates `config.yml`), stop it (or
   use `/ecowatch reload` if you'd rather not restart).
3. Open `plugins/EconomyWatchdog/config.yml`, paste the URL into `discord.webhook-url`, adjust
   `discord.username` and any thresholds under `balance-jump` / `large-transfer` / `auction` to
   taste.
4. `/ecowatch reload`, then `/ecowatch test` to confirm an embed shows up in that channel.

---

## Design notes that apply to both plugins

- Every UDS integration goes through a small `reflect/UdsBridge.java` per plugin, resolved once
  on `onEnable()` — if UDS's method shapes have changed, the plugin logs why and disables itself
  cleanly instead of throwing NPEs later, exactly like CrateBindAddon does.
- Neither plugin adds a hard compile-time dependency on UltimateDonutSmp's jar — only
  `org.bukkit`/Paper API classes are compiled against (`provided` scope), matching how
  CrateBindAddon itself is built.
- Local storage (staff notes) is plain `YamlConfiguration` files under the plugin's own data
  folder — no bundled database driver, so each plugin is a single drop-in jar.

## Getting the jars

Built jars are checked into **`dist/`**:

- `dist/PunishmentHistoryGUI-1.0.0.jar`
- `dist/EconomyWatchdog-1.0.0.jar`

Drop either straight into your server's `plugins/` folder alongside UltimateDonutSmp.

### How they were built without network access to Paper's repo

This sandbox's network policy blocks `repo.papermc.io`, so a normal `mvn package` (which needs
Paper's API jar) can't run here. Instead: `org.bukkit`/Paper/Adventure classes are never bundled
into the jar (they're `provided` at compile time only, same as any real Paper plugin build) — a
plugin jar only ever needs to contain *your own* compiled classes plus `plugin.yml`/`config.yml`,
and the real API classes come from the server at runtime regardless of how you compiled. So each
class was compiled with `javac -Xlint:all` against a hand-written stub reproducing the exact
method signatures used from `org.bukkit`, `org.bukkit.*` and `net.kyori.adventure.*` — zero
errors, zero warnings — and I cross-checked the riskiest of those signatures (the Adventure
`Component`/`sendMessage`/`LegacyComponentSerializer` shapes) against the real
`net.kyori:adventure-api`/`adventure-text-serializer-legacy` jars pulled straight from Maven
Central, which *is* reachable here (that check actually caught and fixed one real bug: an
earlier draft had `LegacyComponentSerializer` modeled as a class — it's really an interface, and
`deserialize(String)` returns `TextComponent`, not `Component`). The remaining Paper-only method
shapes (`ItemMeta#displayName`/`lore`, `Bukkit#createInventory(..., Component)`,
`SkullMeta#setOwningPlayer`) aren't published anywhere reachable from here to verify the same
way, but they've been stable across Paper versions for years.

**Still worth doing before relying on these in production:** a real `mvn package` on a machine
with normal internet access, and a smoke test on a test server — especially exercising
EconomyWatchdog's private-field auction read, which is the one piece of either plugin most
likely to need a small update on a future UltimateDonutSmp version.

To build from source yourself once you have that access:

```
mvn -f pom.xml package
```
