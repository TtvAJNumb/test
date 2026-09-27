package com.donututils.motors.vehicle;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/** NamespacedKeys used both on spawner-key ItemStacks and on the live vehicle/body entities' own
 * PersistentDataContainers, so a vehicle's identity/ownership travels with the real Bukkit objects
 * instead of needing a separate lookup table for everything. */
public final class VehicleKeys {

    public final NamespacedKey vehicleId;
    public final NamespacedKey ownerId;
    public final NamespacedKey bodyMarker;

    public VehicleKeys(Plugin plugin) {
        this.vehicleId = new NamespacedKey(plugin, "vehicle_id");
        this.ownerId = new NamespacedKey(plugin, "owner_id");
        this.bodyMarker = new NamespacedKey(plugin, "motors_body");
    }
}
