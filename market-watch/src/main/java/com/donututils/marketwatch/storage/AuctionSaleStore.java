package com.donututils.marketwatch.storage;

import com.donututils.marketwatch.model.AuctionSaleRecord;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AuctionSaleStore {

    private final File file;
    private final Logger logger;

    public AuctionSaleStore(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "auction-sales.log");
        this.logger = logger;
    }

    public synchronized void append(AuctionSaleRecord record) {
        try {
            Files.createDirectories(file.getParentFile().toPath());
            try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)) {
                writer.write(record.toLine());
                writer.newLine();
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to write auction sale record", ex);
        }
    }

    public synchronized List<AuctionSaleRecord> readAll() {
        List<AuctionSaleRecord> records = new ArrayList<>();
        if (!file.exists()) {
            return records;
        }
        try {
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                AuctionSaleRecord record = AuctionSaleRecord.fromLine(line);
                if (record != null) {
                    records.add(record);
                }
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to read auction sale history", ex);
        }
        return records;
    }

    public synchronized void pruneOlderThan(long cutoffMillis) {
        List<AuctionSaleRecord> kept = new ArrayList<>();
        for (AuctionSaleRecord record : readAll()) {
            if (record.soldAt() >= cutoffMillis) {
                kept.add(record);
            }
        }
        try {
            Files.createDirectories(file.getParentFile().toPath());
            List<String> lines = new ArrayList<>();
            for (AuctionSaleRecord record : kept) {
                lines.add(record.toLine());
            }
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to prune auction sale history", ex);
        }
    }
}
