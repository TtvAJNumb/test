package com.donututils.municipal.claim;

import org.bukkit.Chunk;

/** Identifies one 16x16 chunk, independent of any live Chunk object (which may not be loaded). */
public record ChunkKey(String world, int x, int z) {
    public static ChunkKey of(Chunk chunk) {
        return new ChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }
}
