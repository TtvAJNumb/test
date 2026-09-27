package com.donututils.stockmarket.storage;

import com.donututils.stockmarket.model.TransactionRecord;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/** A single global append-only audit trail of every trade, for admin review. */
public final class TransactionLogStore {

    private final File file;
    private final Logger logger;

    public TransactionLogStore(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "transactions.log");
        this.logger = logger;
    }

    public synchronized void append(TransactionRecord record) {
        try {
            file.getParentFile().mkdirs();
            try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(record.toLine());
                writer.newLine();
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to write transaction log entry", ex);
        }
    }
}
