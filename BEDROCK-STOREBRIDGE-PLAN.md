# Bedrock support for StoreBridge — design plan (not built yet)

Status: **design only**. Nothing described here has been implemented. This file exists so the
plan survives between sessions — build it only when explicitly told to go ahead.

## Why Bedrock needs different handling

Mojang only knows Java accounts. A Bedrock player's identity only exists through Floodgate,
which runs on the Minecraft server, not anywhere Stripe or the backend can reach. So Bedrock
buyers can't be verified at checkout the way Java buyers are — that has to happen later, on the
server, once Floodgate can actually resolve who they are.

## The naming decision

Bedrock players show up in-game with a prefix in front of their name — a dot by default under
Floodgate's standard settings, though that's configurable server-side and could be something
else if it's been changed.

Two options were considered for what a Bedrock buyer types at checkout:
1. They type their plain gamertag, and the plugin adds the dot for them.
2. They type it exactly as it appears in-game, dot included, with nothing corrected for them.

**Option 2 was chosen** — if someone types it wrong, that's clearly on them, not something the
store silently got wrong. The Stripe checkout page already tells Bedrock buyers to include the
dot, so that part is already done.

Asking buyers for their UUID instead of a name (via a command or lookup website) was considered
and ruled out — slower, easier to mistype, and needs new infrastructure that doesn't exist yet.

## The backend (Vercel) — `StoreBridgeBackend` folder

Two code changes:

1. **`lib/orders.js`** — today a name either resolves through Mojang or becomes `needs_review`.
   It needs a third path: a name matching the Bedrock prefix is accepted as `pending` with no
   UUID yet, left for the plugin to resolve, instead of being treated as invalid.
2. **A new endpoint**, something like `/api/plugin/resolve-identity` — once the plugin figures
   out who a Bedrock name really is, it tells the backend, and that UUID gets saved for that
   player's future orders too, wired into `app.js` the same way the existing endpoints are.

   > Worth checking first: StoreBridge already calls an existing `POST /api/plugin/fix-username`
   > endpoint (confirmed from the compiled jar's `BackendClient` class) that takes an order id +
   > username and requeues the job. If that endpoint already persists the resolved UUID for
   > future orders, `resolve-identity` might not need to be a fully separate endpoint. Check
   > `lib/orders.js` for what `fix-username` actually does before building a new one.

Nothing else about the backend changes — same Stripe webhook, same events, same `PLUGIN_KEY` and
database settings already sitting in Vercel.

## The plugin (StoreBridge)

1. **A new Floodgate bridge class**, built the same way as the existing UltimateDonutSmp bridge:
   real methods checked with `javap` against the actual Floodgate jar before any code is written.
   Not done yet — nothing has been verified against a real Floodgate jar.
2. **A join listener** that records every Bedrock player's real identity the moment they join the
   server, not just when they buy something. Most returning players are already known by the
   time they buy anything, so delivery is instant for them.
3. **A small local cache**, similar to the existing delivery `Journal` (confirmed real, a simple
   line-based text file with `S`/`D`/`R` entries) — so the plugin doesn't need to ask Floodgate
   again for someone it already knows.
4. **A brand-new player who buys before ever joining once** is the only case that has to wait.
   The order sits as `pending`, the plugin reports `retry` each check until they log in for the
   first time, then it resolves and delivers automatically — same retry-then-eventually-`failed`
   lifecycle an offline Java player already has today (confirmed real: `Executor.execute()`
   calls `journal.reset(key)` and returns a `"retry"` outcome rather than a permanent `failed`
   when the first command in a chain doesn't go through).
5. **A narrow exception to the existing name-safety check** — confirmed real and exactly this
   strict today: `Executor.NAME = Pattern.compile("[A-Za-z0-9_]{3,16}")`, no dot allowed. The fix
   is one leading dot allowed only for a resolved Bedrock name, without loosening it for anything
   else.

## Config changes

Nothing to the existing `plugin_key` or `backend_url` — both stay exactly as they are. Just four
new lines added into the same `config.yml`:

```yaml
bedrock:
  enabled: true
  prefix: "."
```

## Deployment steps, once it's built

1. `vercel --prod` in the backend folder — existing Vercel settings carry over automatically.
2. `mvn clean package`, then replace the jar in `plugins/`, same as always.
3. Paste the four-line block above into the current `config.yml`.
4. `/storebridge reload` (confirmed real — it's in StoreBridge's own plugin.yml usage string).

## What's confirmed vs. still assumed

**Confirmed against the real compiled StoreBridge.jar** (via CFR decompile + javap during this
project): the exact name-validation regex, the retry/pending/failed lifecycle, the `Journal`
file format, the `/storebridge reload` command, and that a `fix-username` endpoint already
exists in the backend API surface.

**Not yet confirmed:** the exact Floodgate/Geyser method signatures the new bridge would call —
that check happens before any Floodgate-facing code gets written, same standard as every other
reflection bridge built in this project. Also not seen: the actual `lib/orders.js` /`app.js`
source, so the backend section above is a plan to verify against that source, not a diff against
it.
