package com.donututils.realworld.stockmarket.storage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Per-symbol index of which players currently hold at least one share - lets dividend payouts and
 * splits find every affected player without scanning every portfolio file on disk.
 */
public final class HolderIndexStore {

    private final File holdersDirectory;
    private final Logger logger;

    public HolderIndexStore(File dataFolder, Logger logger) {
        this.holdersDirectory = new File(dataFolder, "holders");
        this.logger = logger;
    }

    public synchronized Set<UUID> getHolders(String symbol) {
        Set<UUID> holders = new LinkedHashSet<>();
        File file = fileFor(symbol);
        if (!file.exists()) {
            return holders;
        }
        try {
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                try {
                    holders.add(UUID.fromString(line.trim()));
                } catch (IllegalArgumentException ignored) {
                    // skip malformed line
                }
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to read holder index for " + symbol, ex);
        }
        return holders;
    }

    public synchronized void addHolder(String symbol, UUID playerId) {
        Set<UUID> holders = getHolders(symbol);
        if (holders.add(playerId)) {
            write(symbol, holders);
        }
    }

    public synchronized void removeHolder(String symbol, UUID playerId) {
        Set<UUID> holders = getHolders(symbol);
        if (holders.remove(playerId)) {
            write(symbol, holders);
        }
    }

    private void write(String symbol, Set<UUID> holders) {
        try {
            Files.createDirectories(holdersDirectory.toPath());
            StringBuilder sb = new StringBuilder();
            for (UUID id : holders) {
                sb.append(id).append('\n');
            }
            Files.writeString(fileFor(symbol).toPath(), sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to write holder index for " + symbol, ex);
        }
    }

    public synchronized void clearAll(String symbol) {
        File file = fileFor(symbol);
        if (file.exists() && !file.delete()) {
            logger.log(Level.WARNING, "Failed to delete holder index for " + symbol);
        }
    }

    private File fileFor(String symbol) {
        return new File(holdersDirectory, symbol.toUpperCase() + ".txt");
    }
}
