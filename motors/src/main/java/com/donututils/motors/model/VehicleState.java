package com.donututils.motors.model;

import org.bukkit.Location;

import java.util.UUID;

/** In-memory state for one placed vehicle. Reconnected to its real Boat/Minecart entity lazily
 * (by UUID, via World#getEntity) since vanilla chunk serialization already persists that entity
 * across restarts - Motors never has to save/restore the entity itself, only these numbers. */
public final class VehicleState {

    public final UUID entityId;
    public final String vehicleDefinitionId;
    public final UUID ownerId;

    public double fuel;
    public double wear;
    public double mileage;

    public String worldName;
    public Location lastLocation;
    public UUID bodyEntityId;

    public VehicleState(UUID entityId, String vehicleDefinitionId, UUID ownerId, double fuel, double wear,
                         double mileage, String worldName, UUID bodyEntityId) {
        this.entityId = entityId;
        this.vehicleDefinitionId = vehicleDefinitionId;
        this.ownerId = ownerId;
        this.fuel = fuel;
        this.wear = wear;
        this.mileage = mileage;
        this.worldName = worldName;
        this.bodyEntityId = bodyEntityId;
    }
}
