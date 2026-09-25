package com.donututils.punishhistory.reflect;

import com.donututils.punishhistory.model.PunishmentSnapshot;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Talks to UltimateDonutSmp's internal PunishmentManager purely by reflection, the same
 * approach CrateBindAddon uses for the crate API. There is no compile-time API jar for
 * addons, so method handles are resolved once on enable and this plugin disables itself
 * with a clear message if a UDS update has changed the shape we depend on.
 */
public final class UdsBridge {

    private final Plugin uds;
    private final Method getPunishmentManager;
    private final Method resolveTargetUuid;
    private final Method resolveTargetNameMethod;
    private final Method getAll;
    private final Method countAll;
    private final Method getHistory;
    private final Method countHistory;
    private final Method getDisplayType;
    private final Method getState;

    private UdsBridge(Plugin uds) throws ReflectiveOperationException {
        this.uds = uds;
        Class<?> pluginClass = uds.getClass();
        ClassLoader loader = pluginClass.getClassLoader();

        this.getPunishmentManager = pluginClass.getMethod("getPunishmentManager");
        Class<?> managerClass = this.getPunishmentManager.getReturnType();

        Class<?> queryClass = Class.forName("com.bx.ultimateDonutSmp.models.PunishmentQuery", false, loader);
        Class<?> recordClass = Class.forName("com.bx.ultimateDonutSmp.models.PunishmentRecord", false, loader);

        this.resolveTargetUuid = managerClass.getMethod("resolveTargetUuid", String.class, boolean.class);
        this.resolveTargetNameMethod = managerClass.getMethod("resolveTargetName", UUID.class);
        this.getAll = managerClass.getMethod("getAll", queryClass, String.class, int.class, int.class);
        this.countAll = managerClass.getMethod("countAll", queryClass, String.class);
        this.getHistory = managerClass.getMethod("getHistory", UUID.class, String.class, int.class, int.class, queryClass);
        this.countHistory = managerClass.getMethod("countHistory", UUID.class, String.class, queryClass);
        this.getDisplayType = managerClass.getMethod("getDisplayType", recordClass);
        this.getState = managerClass.getMethod("getState", recordClass);
    }

    public static UdsBridge create(Plugin uds) throws ReflectiveOperationException {
        return new UdsBridge(uds);
    }

    @SuppressWarnings("unchecked")
    public Optional<UUID> resolveTargetUuid(String username) {
        Object result = call(resolveTargetUuid, manager(), username, Boolean.TRUE);
        return result instanceof Optional<?> opt ? (Optional<UUID>) opt : Optional.empty();
    }

    public String resolveTargetName(UUID uuid) {
        return (String) call(resolveTargetNameMethod, manager(), uuid);
    }

    public int countAll(String search) {
        return (int) call(countAll, manager(), null, search);
    }

    public List<PunishmentSnapshot> getAll(String search, int limit, int offset) throws ReflectiveOperationException {
        Object result = call(getAll, manager(), null, search, limit, offset);
        return toSnapshots((List<?>) result);
    }

    public int countHistory(UUID uuid, String name) {
        return (int) call(countHistory, manager(), uuid, name, null);
    }

    public List<PunishmentSnapshot> getHistory(UUID uuid, String name, int limit, int offset) throws ReflectiveOperationException {
        Object result = call(getHistory, manager(), uuid, name, limit, offset, null);
        return toSnapshots((List<?>) result);
    }

    private List<PunishmentSnapshot> toSnapshots(List<?> raw) throws ReflectiveOperationException {
        List<PunishmentSnapshot> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (Object record : raw) {
            out.add(toSnapshot(record));
        }
        return out;
    }

    private PunishmentSnapshot toSnapshot(Object record) throws ReflectiveOperationException {
        long id = (long) invoke(record, "getId");
        UUID targetUuid = (UUID) invoke(record, "getTargetUuid");
        String targetName = (String) invoke(record, "getTargetNameSnapshot");

        Object typeObj = invoke(record, "getType");
        String type = typeObj == null ? "UNKNOWN" : (String) invoke(typeObj, "name");

        String reason = (String) invoke(record, "getReason");
        String issuer = (String) invoke(record, "getIssuerNameSnapshot");
        long issuedAt = (long) invoke(record, "getIssuedAt");

        Object expiresObj = invoke(record, "getExpiresAt");
        Long expiresAt = expiresObj instanceof Long l ? l : null;

        boolean removed = (boolean) invoke(record, "isRemoved");
        String removedBy = (String) invoke(record, "getRemovedByNameSnapshot");
        String removalReason = (String) invoke(record, "getRemovalReason");

        String displayType = (String) call(getDisplayType, manager(), record);
        Object stateObj = call(getState, manager(), record);
        String state = stateObj == null ? "UNKNOWN" : (String) invoke(stateObj, "name");

        return new PunishmentSnapshot(id, targetUuid, targetName, type, displayType, reason, issuer,
                issuedAt, expiresAt, removed, removedBy, removalReason, state);
    }

    private Object manager() {
        Object manager = call(getPunishmentManager, uds);
        if (manager == null) {
            throw new IllegalStateException("UltimateDonutSmp punishment manager is not ready yet");
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
