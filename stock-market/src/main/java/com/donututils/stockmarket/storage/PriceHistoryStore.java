package com.donututils.stockmarket.storage;

import com.donututils.stockmarket.model.PriceTick;

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

/** One append-only pipe-delimited log file per stock symbol, same shape as MarketWatch's stores. */
public final class PriceHistoryStore {

    private final File historyDirectory;
    private final Logger logger;

    public PriceHistoryStore(File dataFolder, Logger logger) {
        this.historyDirectory = new File(dataFolder, "price-history");
        this.logger = logger;
    }

    public synchronized void append(String symbol, PriceTick tick) {
        try {
            Files.createDirectories(historyDirectory.toPath());
            File file = fileFor(symbol);
            try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(tick.toLine());
                writer.newLine();
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to write price history for " + symbol, ex);
        }
    }

    public synchronized List<PriceTick> readAll(String symbol) {
        List<PriceTick> ticks = new ArrayList<>();
        File file = fileFor(symbol);
        if (!file.exists()) {
            return ticks;
        }
        try {
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                PriceTick tick = PriceTick.fromLine(line);
                if (tick != null) {
                    ticks.add(tick);
                }
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to read price history for " + symbol, ex);
        }
        return ticks;
    }

    public synchronized void pruneOlderThan(String symbol, long cutoffMillis) {
        List<PriceTick> kept = new ArrayList<>();
        for (PriceTick tick : readAll(symbol)) {
            if (tick.timestampMillis() >= cutoffMillis) {
                kept.add(tick);
            }
        }
        try {
            Files.createDirectories(historyDirectory.toPath());
            List<String> lines = new ArrayList<>();
            for (PriceTick tick : kept) {
                lines.add(tick.toLine());
            }
            Files.write(fileFor(symbol).toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to prune price history for " + symbol, ex);
        }
    }

    private File fileFor(String symbol) {
        return new File(historyDirectory, symbol.toUpperCase() + ".log");
    }
}
