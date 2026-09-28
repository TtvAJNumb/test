package com.donututils.realworld.dynamicshop.storage;

import com.donututils.realworld.dynamicshop.model.TransactionRecord;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Append-only transaction history, same pipe-delimited-log pattern used elsewhere in this repo. */
public final class TransactionLogStore {

    private final File file;
    private final Logger logger;

    public TransactionLogStore(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "transactions.log");
        this.logger = logger;
    }

    public synchronized void append(TransactionRecord record) {
        try {
            Files.createDirectories(file.getParentFile().toPath());
            try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(record.toLine());
                writer.newLine();
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to write transaction record", ex);
        }
    }

    public synchronized List<TransactionRecord> readAll() {
        List<TransactionRecord> records = new ArrayList<>();
        if (!file.exists()) {
            return records;
        }
        try {
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                TransactionRecord record = TransactionRecord.fromLine(line);
                if (record != null) {
                    records.add(record);
                }
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to read transaction history", ex);
        }
        return records;
    }
}
