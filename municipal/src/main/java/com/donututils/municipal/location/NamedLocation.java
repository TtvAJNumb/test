package com.donututils.municipal.location;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/** A staff-named place: always has a teleport point, and optionally a cuboid region (e.g. jail
 * bounds) for containment checks. */
public final class NamedLocation {

    private final String name;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final Cuboid bounds;

    public NamedLocation(String name, String world, double x, double y, double z, float yaw, float pitch, Cuboid bounds) {
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.bounds = bounds;
    }

    public String name() {
        return name;
    }

    public String world() {
        return world;
    }

    public Cuboid bounds() {
        return bounds;
    }

    public boolean hasBounds() {
        return bounds != null;
    }

    /** Null if the world isn't currently loaded. */
    public Location toLocation() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : new Location(w, x, y, z, yaw, pitch);
    }
}
