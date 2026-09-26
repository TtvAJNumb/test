package com.donututils.teamleaderboard.reflect;

import com.donututils.teamleaderboard.model.StatKind;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Talks to UltimateDonutSmp's real, public TeamManager and LeaderboardManager by reflection
 * (verified against the plugin's actual source, not decompiled guesswork - every method resolved
 * here is a genuine public method on the real classes). Nothing here reaches into a private field.
 */
public final class UdsBridge {

    private final Plugin uds;

    private final Method getTeamManager;
    private final Method getAllTeams;
    private final Method teamGetName;
    private final Method teamGetLeaderUuid;
    private final Method teamGetMemberUuids;
    private final Method teamGetMemberCount;

    private final Method getLeaderboardManager;
    private final Method getEntries;
    private final Method getTotalEntries;
    private final Method entryPlayerData;
    private final Method playerDataGetUuid;
    private final Method playerDataGetMoney;
    private final Method playerDataGetShards;
    private final Object moneyType;
    private final Object shardsType;

    private UdsBridge(Plugin uds) throws ReflectiveOperationException {
        this.uds = uds;
        Class<?> pluginClass = uds.getClass();

        this.getTeamManager = pluginClass.getMethod("getTeamManager");
        Class<?> teamManagerClass = getTeamManager.getReturnType();
        this.getAllTeams = teamManagerClass.getMethod("getAllTeams");

        Class<?> teamClass = Class.forName("com.bx.ultimateDonutSmp.models.Team");
        this.teamGetName = teamClass.getMethod("getName");
        this.teamGetLeaderUuid = teamClass.getMethod("getLeaderUuid");
        this.teamGetMemberUuids = teamClass.getMethod("getMemberUuids");
        this.teamGetMemberCount = teamClass.getMethod("getMemberCount");

        this.getLeaderboardManager = pluginClass.getMethod("getLeaderboardManager");
        Class<?> leaderboardManagerClass = getLeaderboardManager.getReturnType();
        Class<?> leaderboardTypeClass = Class.forName(leaderboardManagerClass.getName() + "$LeaderboardType");
        Method valueOf = leaderboardTypeClass.getMethod("valueOf", String.class);
        this.moneyType = valueOf.invoke(null, StatKind.MONEY.getUdsLeaderboardTypeName());
        this.shardsType = valueOf.invoke(null, StatKind.SHARDS.getUdsLeaderboardTypeName());

        this.getEntries = leaderboardManagerClass.getMethod("getEntries", leaderboardTypeClass, int.class, int.class);
        this.getTotalEntries = leaderboardManagerClass.getMethod("getTotalEntries", leaderboardTypeClass);

        Class<?> entryClass = Class.forName(leaderboardManagerClass.getName() + "$LeaderboardEntry");
        this.entryPlayerData = entryClass.getMethod("playerData");

        Class<?> playerDataClass = Class.forName("com.bx.ultimateDonutSmp.models.PlayerData");
        this.playerDataGetUuid = playerDataClass.getMethod("getUuid");
        this.playerDataGetMoney = playerDataClass.getMethod("getMoney");
        this.playerDataGetShards = playerDataClass.getMethod("getShards");
    }

    public static UdsBridge create(Plugin uds) throws ReflectiveOperationException {
        return new UdsBridge(uds);
    }

    public record TeamInfo(String name, UUID leaderUuid, int memberCount, List<UUID> memberUuids) {
    }

    @SuppressWarnings("unchecked")
    public List<TeamInfo> getTeams() {
        Object teamManager = call(getTeamManager, uds);
        Object raw = call(getAllTeams, teamManager);
        List<TeamInfo> teams = new ArrayList<>();
        for (Object team : (Collection<Object>) raw) {
            String name = (String) call(teamGetName, team);
            UUID leaderUuid = (UUID) call(teamGetLeaderUuid, team);
            int memberCount = (int) call(teamGetMemberCount, team);
            Collection<UUID> memberUuids = (Collection<UUID>) call(teamGetMemberUuids, team);
            teams.add(new TeamInfo(name, leaderUuid, memberCount, List.copyOf(memberUuids)));
        }
        return teams;
    }

    /** Every known player's current value for the given stat, keyed by UUID - covers offline players too. */
    @SuppressWarnings("unchecked")
    public Map<UUID, Double> getMoneyByPlayer() {
        return getValuesByPlayer(moneyType, playerDataGetMoney, value -> ((Number) value).doubleValue());
    }

    @SuppressWarnings("unchecked")
    public Map<UUID, Long> getShardsByPlayer() {
        return getValuesByPlayer(shardsType, playerDataGetShards, value -> ((Number) value).longValue());
    }

    @SuppressWarnings("unchecked")
    private <T> Map<UUID, T> getValuesByPlayer(Object type, Method valueGetter, java.util.function.Function<Object, T> convert) {
        Object leaderboardManager = call(getLeaderboardManager, uds);
        int total = (int) call(getTotalEntries, leaderboardManager, type);
        List<Object> entries = (List<Object>) call(getEntries, leaderboardManager, type, 0, total);

        Map<UUID, T> values = new HashMap<>();
        for (Object entry : entries) {
            Object playerData = call(entryPlayerData, entry);
            UUID uuid = (UUID) call(playerDataGetUuid, playerData);
            Object rawValue = call(valueGetter, playerData);
            if (uuid != null) {
                values.put(uuid, convert.apply(rawValue));
            }
        }
        return values;
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
