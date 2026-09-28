package com.donututils.realworld.motors.vehicle;

import com.donututils.realworld.motors.config.MotorsConfig;
import com.donututils.realworld.motors.config.VehicleDefinition;
import com.donututils.realworld.motors.db.DatabaseManager;
import com.donututils.realworld.motors.economy.VaultEconomyBridge;
import com.donututils.realworld.motors.model.VehicleState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Owns every placed vehicle's runtime state. Movement/steering is 100% vanilla Boat/Minecart
 * physics - this class never touches velocity itself. It only meters fuel while a vehicle is
 * occupied, accrues wear from distance traveled, ejects riders when a vehicle runs dry or breaks
 * down, and keeps a decorative ArmorStand ("body") glued to the vehicle's location so a resource
 * pack can skin it as an actual car/train instead of a boat/minecart.
 *
 * <p>Ownership and fuel/wear survive restarts without Motors ever persisting the vehicle entity
 * itself: vanilla chunk serialization already does that (stable UUID included), so on boot this
 * class only reloads its own numbers from SQLite and reconnects to the live entity lazily, once
 * its chunk is loaded, via {@link World#getEntity(UUID)}.
 */
public final class VehicleManager {

    private final Plugin plugin;
    private final DatabaseManager db;
    private final VehicleKeys keys;
    private final VehicleItemFactory itemFactory;
    private final Supplier<MotorsConfig> configSupplier;

    private final Map<UUID, VehicleState> states = new ConcurrentHashMap<>();

    public VehicleManager(Plugin plugin, DatabaseManager db, VehicleKeys keys, VehicleItemFactory itemFactory,
                           Supplier<MotorsConfig> configSupplier) {
        this.plugin = plugin;
        this.db = db;
        this.keys = keys;
        this.itemFactory = itemFactory;
        this.configSupplier = configSupplier;
    }

    /** Reloads every vehicle's fuel/wear/mileage from disk. Call once during onEnable, before the
     * tick task starts - deliberately synchronous since it's a one-time local SQLite read at boot. */
    public void loadFromDatabase() {
        String sql = "SELECT entity_uuid, owner_id, vehicle_id, body_uuid, fuel, wear, mileage, world, x, y, z FROM vehicles";
        try (Connection connection = db.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                UUID entityId = UUID.fromString(rs.getString("entity_uuid"));
                UUID ownerId = UUID.fromString(rs.getString("owner_id"));
                String bodyUuidRaw = rs.getString("body_uuid");
                UUID bodyId = bodyUuidRaw != null ? UUID.fromString(bodyUuidRaw) : null;
                String worldName = rs.getString("world");

                VehicleState state = new VehicleState(entityId, rs.getString("vehicle_id"), ownerId,
                        rs.getDouble("fuel"), rs.getDouble("wear"), rs.getDouble("mileage"), worldName, bodyId);
                state.lastLocation = new Location(Bukkit.getWorld(worldName), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"));
                states.put(entityId, state);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load vehicles from database", ex);
        }
        plugin.getLogger().info("Loaded " + states.size() + " vehicle(s) from database.");
    }

    /** Places a brand-new vehicle at the given location and starts tracking it. */
    public Entity spawn(Player owner, VehicleDefinition definition, Location at) {
        Entity vehicleEntity = definition.kind() == VehicleDefinition.Kind.BOAT
                ? at.getWorld().spawn(at, Boat.class)
                : at.getWorld().spawn(at, Minecart.class);

        vehicleEntity.getPersistentDataContainer().set(keys.vehicleId, PersistentDataType.STRING, definition.id());
        vehicleEntity.getPersistentDataContainer().set(keys.ownerId, PersistentDataType.STRING, owner.getUniqueId().toString());

        VehicleState state = new VehicleState(vehicleEntity.getUniqueId(), definition.id(), owner.getUniqueId(),
                definition.maxFuel(), 0.0, 0.0, at.getWorld().getName(), null);
        state.lastLocation = at;
        states.put(state.entityId, state);
        persistAsync(state);
        return vehicleEntity;
    }

    /** Runs once per second on the main thread (entity access requires it). Meters fuel, accrues
     * wear/mileage, ejects riders past a hard limit, and keeps each vehicle's decorative body
     * glued to its real location. */
    public void tick() {
        for (VehicleState state : states.values()) {
            VehicleDefinition definition = configSupplier.get().vehicle(state.vehicleDefinitionId);
            if (definition == null) {
                continue;
            }
            World world = Bukkit.getWorld(state.worldName);
            if (world == null) {
                continue;
            }
            Entity vehicleEntity = world.getEntity(state.entityId);
            if (vehicleEntity == null) {
                continue;
            }

            Location current = vehicleEntity.getLocation();
            if (state.lastLocation != null && current.getWorld() == state.lastLocation.getWorld()) {
                double distance = current.distance(state.lastLocation);
                if (distance > 0.01 && distance < 50.0) {
                    state.mileage += distance;
                    state.wear = Math.min(definition.maxWear(), state.wear + distance * definition.wearPerBlock());
                }
            }
            state.lastLocation = current;
            state.worldName = world.getName();

            List<Entity> passengers = vehicleEntity.getPassengers();
            boolean occupied = !passengers.isEmpty();
            if (occupied) {
                state.fuel = Math.max(0.0, state.fuel - definition.fuelDrainPerSecond());
            }

            if (state.fuel <= 0.0 || state.wear >= definition.maxWear()) {
                String reason = state.fuel <= 0.0
                        ? "&cYour " + definition.displayName() + "&c is out of fuel."
                        : "&cYour " + definition.displayName() + "&c needs repair - it broke down.";
                for (Entity passenger : List.copyOf(passengers)) {
                    vehicleEntity.removePassenger(passenger);
                    if (passenger instanceof Player player) {
                        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', reason));
                    }
                }
            }

            syncBody(world, current, definition, state);
            persistAsync(state);
        }
    }

    private void syncBody(World world, Location current, VehicleDefinition definition, VehicleState state) {
        Entity body = state.bodyEntityId != null ? world.getEntity(state.bodyEntityId) : null;
        if (body == null) {
            ArmorStand stand = world.spawn(current, ArmorStand.class);
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setSmall(true);
            stand.setBasePlate(false);
            stand.setGravity(false);
            stand.setHelmet(itemFactory.createBodyItem(definition));
            stand.getPersistentDataContainer().set(keys.bodyMarker, PersistentDataType.STRING, definition.id());
            state.bodyEntityId = stand.getUniqueId();
        } else {
            body.teleport(current);
        }
    }

    public VehicleState findRiddenState(Player player) {
        Entity vehicle = player.getVehicle();
        if (vehicle == null) {
            return null;
        }
        return states.get(vehicle.getUniqueId());
    }

    public enum EconomyOutcome {
        SUCCESS,
        NOT_IN_VEHICLE,
        UNKNOWN_VEHICLE_TYPE,
        INSUFFICIENT_FUNDS,
        FUEL_ALREADY_FULL,
        NO_WEAR
    }

    public EconomyOutcome refuel(Player player, double amount, VaultEconomyBridge economy) {
        VehicleState state = findRiddenState(player);
        if (state == null) {
            return EconomyOutcome.NOT_IN_VEHICLE;
        }
        VehicleDefinition definition = configSupplier.get().vehicle(state.vehicleDefinitionId);
        if (definition == null) {
            return EconomyOutcome.UNKNOWN_VEHICLE_TYPE;
        }
        if (state.fuel >= definition.maxFuel()) {
            return EconomyOutcome.FUEL_ALREADY_FULL;
        }
        double applied = Math.min(amount, definition.maxFuel() - state.fuel);
        double cost = applied * definition.refuelCostPerUnit();
        if (!economy.has(player.getUniqueId(), cost)) {
            return EconomyOutcome.INSUFFICIENT_FUNDS;
        }
        VaultEconomyBridge.EconomyResult result = economy.withdraw(player.getUniqueId(), cost);
        if (!result.success()) {
            return EconomyOutcome.INSUFFICIENT_FUNDS;
        }
        state.fuel += applied;
        persistAsync(state);
        return EconomyOutcome.SUCCESS;
    }

    public EconomyOutcome repair(Player player, VaultEconomyBridge economy) {
        VehicleState state = findRiddenState(player);
        if (state == null) {
            return EconomyOutcome.NOT_IN_VEHICLE;
        }
        VehicleDefinition definition = configSupplier.get().vehicle(state.vehicleDefinitionId);
        if (definition == null) {
            return EconomyOutcome.UNKNOWN_VEHICLE_TYPE;
        }
        if (state.wear <= 0.0) {
            return EconomyOutcome.NO_WEAR;
        }
        double cost = state.wear * definition.repairCostPerWear();
        if (!economy.has(player.getUniqueId(), cost)) {
            return EconomyOutcome.INSUFFICIENT_FUNDS;
        }
        VaultEconomyBridge.EconomyResult result = economy.withdraw(player.getUniqueId(), cost);
        if (!result.success()) {
            return EconomyOutcome.INSUFFICIENT_FUNDS;
        }
        state.wear = 0.0;
        persistAsync(state);
        return EconomyOutcome.SUCCESS;
    }

    public VehicleState getState(UUID entityId) {
        return states.get(entityId);
    }

    private void persistAsync(VehicleState state) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> upsert(state));
    }

    private void upsert(VehicleState state) {
        String sql = """
                INSERT INTO vehicles (entity_uuid, owner_id, vehicle_id, body_uuid, fuel, wear, mileage, world, x, y, z, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(entity_uuid) DO UPDATE SET
                    fuel = excluded.fuel,
                    wear = excluded.wear,
                    mileage = excluded.mileage,
                    body_uuid = excluded.body_uuid,
                    world = excluded.world,
                    x = excluded.x,
                    y = excluded.y,
                    z = excluded.z
                """;
        try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.entityId.toString());
            statement.setString(2, state.ownerId.toString());
            statement.setString(3, state.vehicleDefinitionId);
            statement.setString(4, state.bodyEntityId != null ? state.bodyEntityId.toString() : null);
            statement.setDouble(5, state.fuel);
            statement.setDouble(6, state.wear);
            statement.setDouble(7, state.mileage);
            statement.setString(8, state.worldName);
            Location loc = state.lastLocation;
            statement.setDouble(9, loc != null ? loc.getX() : 0);
            statement.setDouble(10, loc != null ? loc.getY() : 0);
            statement.setDouble(11, loc != null ? loc.getZ() : 0);
            statement.setLong(12, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to persist vehicle " + state.entityId, ex);
        }
    }

    /** Synchronous final save on shutdown, so the last second of driving isn't lost. */
    public void shutdownFlush() {
        for (VehicleState state : states.values()) {
            upsert(state);
        }
    }
}
