package com.donututils.marketwatch.storage;

import com.donututils.marketwatch.model.EconomySnapshot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Plain append-only file of pipe-delimited lines, one snapshot per line. Deliberately not a real
 * database (no new dependency to shade) - this plugin's write volume is one line per sweep, which
 * a flat file handles fine.
 */
public final class EconomySnapshotStore {

    private final File file;
    private final Logger logger;

    public EconomySnapshotStore(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "economy-history.log");
        this.logger = logger;
    }

    public synchronized void append(EconomySnapshot snapshot) {
        try {
            Files.createDirectories(file.getParentFile().toPath());
            try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)) {
                writer.write(snapshot.toLine());
                writer.newLine();
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to write economy snapshot", ex);
        }
    }

    public synchronized List<EconomySnapshot> readAll() {
        List<EconomySnapshot> snapshots = new ArrayList<>();
        if (!file.exists()) {
            return snapshots;
        }
        try {
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                EconomySnapshot snapshot = EconomySnapshot.fromLine(line);
                if (snapshot != null) {
                    snapshots.add(snapshot);
                }
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to read economy history", ex);
        }
        return snapshots;
    }

    public synchronized void pruneOlderThan(long cutoffMillis) {
        List<EconomySnapshot> kept = new ArrayList<>();
        for (EconomySnapshot snapshot : readAll()) {
            if (snapshot.timestampMillis() >= cutoffMillis) {
                kept.add(snapshot);
            }
        }
        rewrite(kept);
    }

    private void rewrite(List<EconomySnapshot> snapshots) {
        try {
            Files.createDirectories(file.getParentFile().toPath());
            List<String> lines = new ArrayList<>();
            for (EconomySnapshot snapshot : snapshots) {
                lines.add(snapshot.toLine());
            }
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
