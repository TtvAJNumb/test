package com.donututils.municipal.location;

import org.bukkit.Location;

/** A simple axis-aligned box selection between two corners, normalized so min <= max on every
 * axis regardless of which corner the player picked first. */
public record Cuboid(String world, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {

    public static Cuboid of(Location a, Location b) {
        String world = a.getWorld().getName();
        return new Cuboid(world,
                Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }

    public boolean contains(Location location) {
        if (location.getWorld() == null || !location.getWorld().getName().equals(world)) {
            return false;
        }
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public double centerX() {
        return (minX + maxX) / 2.0;
    }

    public double centerZ() {
        return (minZ + maxZ) / 2.0;
    }
}
