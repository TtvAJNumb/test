package com.donututils.crateodds.reflect;

import com.donututils.crateodds.model.CrateSnapshot;
import com.donututils.crateodds.model.RewardOdds;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Talks to UltimateDonutSmp's internal CrateManager purely by reflection, the same way
 * CrateBindAddon's own UdsBridge does. UDS does not ship a compile-time API jar for addons,
 * so we resolve the methods we need at runtime and fail (loudly, once, on enable) if a UDS
 * update has renamed something.
 */
public final class UdsBridge {

    private final Plugin uds;
    private final Method getCrateManager;
    private final Method isBindableBlock;
    private final Method getBoundCrateId;
    private final Method getCrate;
    private final Method getSession;
    private final Method getCrates;

    private UdsBridge(Plugin uds) throws ReflectiveOperationException {
        this.uds = uds;
        Class<?> pluginClass = uds.getClass();
        this.getCrateManager = pluginClass.getMethod("getCrateManager");
        Class<?> managerClass = this.getCrateManager.getReturnType();
        this.isBindableBlock = managerClass.getMethod("isBindableBlock", Material.class);
        this.getBoundCrateId = managerClass.getMethod("getBoundCrateId", Block.class);
        this.getCrate = managerClass.getMethod("getCrate", String.class);
        this.getSession = managerClass.getMethod("getSession", UUID.class);
        this.getCrates = managerClass.getMethod("getCrates");
    }

    public static UdsBridge create(Plugin uds) throws ReflectiveOperationException {
        return new UdsBridge(uds);
    }

    public boolean isBindable(Material material) {
        return (Boolean) call(isBindableBlock, crateManager(), material);
    }

    /** Mirrors CrateBindAddon: reads the crate id UDS itself has bound to this block. */
    public String boundCrateId(Block block) {
        return (String) call(getBoundCrateId, crateManager(), block);
    }

    public CrateSnapshot getCrateSnapshot(String crateId) throws ReflectiveOperationException {
        Object definition = getCrateDefinitionRaw(crateId);
        if (definition == null) {
            return null;
        }
        return toSnapshot(definition);
    }

    /** Returns the raw CrateManager.CrateDefinition record for a crate id, or null if unknown. */
    public Object getCrateDefinitionRaw(String crateId) {
        return call(getCrate, crateManager(), crateId);
    }

    /** Every configured crate id, for tab-completion and listing. */
    public List<String> getCrateIds() {
        Object result = call(getCrates, crateManager());
        List<String> ids = new ArrayList<>();
        if (result instanceof Collection<?> collection) {
            for (Object definition : collection) {
                try {
                    Object id = invoke(definition, "id");
                    if (id != null) {
                        ids.add(id.toString());
                    }
                } catch (ReflectiveOperationException ignored) {
                    // skip malformed entries rather than failing the whole listing
                }
            }
        }
        return ids;
    }

    /** Returns the raw CrateManager.CrateOpenSession for a player, or null if they have none open. */
    public Object getSessionRaw(UUID uuid) {
        return call(getSession, crateManager(), uuid);
    }

    /** Extracts CrateOpenSession#selectedReward() (nullable) via reflection. */
    public Object getSelectedReward(Object session) throws ReflectiveOperationException {
        return invoke(session, "selectedReward");
    }

    /** Extracts CrateOpenSession#crate() -> CrateDefinition, then reads its id(). */
    public String getSessionCrateId(Object session) throws ReflectiveOperationException {
        Object crateDefinition = invoke(session, "crate");
        return crateDefinition == null ? null : (String) invoke(crateDefinition, "id");
    }

    public RewardOdds toRewardOdds(Object crateDefinition, Object reward) throws ReflectiveOperationException {
        long totalWeight = totalWeight(crateDefinition);
        return buildRewardOdds(reward, totalWeight);
    }

    private long totalWeight(Object crateDefinition) throws ReflectiveOperationException {
        if (crateDefinition == null) {
            return 0L;
        }
        List<?> rewards = (List<?>) invoke(crateDefinition, "rewards");
        long total = 0L;
        for (Object reward : rewards) {
            int weight = (int) invoke(reward, "weight");
            if (weight > 0) {
                total += weight;
            }
        }
        return total;
    }

    private CrateSnapshot toSnapshot(Object crateDefinition) throws ReflectiveOperationException {
        String id = (String) invoke(crateDefinition, "id");
        List<?> rewardsRaw = (List<?>) invoke(crateDefinition, "rewards");
        long totalWeight = totalWeight(crateDefinition);

        List<RewardOdds> rewards = new ArrayList<>();
        for (Object reward : rewardsRaw) {
            rewards.add(buildRewardOdds(reward, totalWeight));
        }
        rewards.sort(Comparator.comparingDouble(RewardOdds::percent).reversed());
        return new CrateSnapshot(id, rewards);
    }

    private RewardOdds buildRewardOdds(Object reward, long totalWeight) throws ReflectiveOperationException {
        String rewardId = (String) invoke(reward, "id");
        int weight = (int) invoke(reward, "weight");
        Object display = invoke(reward, "display");

        Material material = Material.CHEST;
        String displayName = rewardId;
        List<String> lore = List.of();
        int amount = 1;
        if (display != null) {
            Object materialObj = invoke(display, "material");
            if (materialObj instanceof Material m) {
                material = m;
            }
            Object nameObj = invoke(display, "displayName");
            if (nameObj instanceof String s && !s.isBlank()) {
                displayName = s;
            }
            Object loreObj = invoke(display, "lore");
            if (loreObj instanceof List<?> list) {
                lore = castStringList(list);
            }
            Object amountObj = invoke(display, "amount");
            if (amountObj instanceof Integer i) {
                amount = i;
            }
        }

        Object grant = invoke(reward, "grant");
        String summary = summarizeGrant(grant);
        double percent = totalWeight > 0 && weight > 0 ? (weight * 100.0 / totalWeight) : 0.0;

        return new RewardOdds(rewardId, displayName, lore, material, amount, weight, percent, summary);
    }

    private String summarizeGrant(Object grant) throws ReflectiveOperationException {
        if (grant == null) {
            return "Unknown reward";
        }
        Object type = invoke(grant, "type");
        String typeName = type == null ? "" : (String) invoke(type, "name");
        return switch (typeName) {
            case "MONEY" -> "$" + formatNumber(((Number) invoke(grant, "moneyAmount")).doubleValue());
            case "SHARDS" -> formatNumber(((Number) invoke(grant, "shardAmount")).doubleValue()) + " shards";
            case "ITEM" -> "Item reward";
            case "COMMAND" -> "Command reward";
            default -> "Reward";
        };
    }

    private static String formatNumber(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.format(Locale.US, "%,.0f", value);
        }
        return String.format(Locale.US, "%,.2f", value);
    }

    @SuppressWarnings("unchecked")
    private static List<String> castStringList(List<?> raw) {
        List<String> out = new ArrayList<>();
        for (Object o : raw) {
            out.add(String.valueOf(o));
        }
        return out;
    }

    private Object crateManager() {
        Object manager = call(getCrateManager, uds);
        if (manager == null) {
            throw new IllegalStateException("UltimateDonutSmp crate manager is not ready yet");
        }
        return manager;
    }

    private static Object invoke(Object target, String noArgMethodName) throws ReflectiveOperationException {
        if (target == null) {
            return null;
        }
        Method method = target.getClass().getMethod(noArgMethodName);
        return call(method, target);
    }

    private static Object call(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new IllegalStateException(method.getName() + " failed: " + cause, cause);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(method.getName() + " is not accessible: " + ex, ex);
        }
    }
}
