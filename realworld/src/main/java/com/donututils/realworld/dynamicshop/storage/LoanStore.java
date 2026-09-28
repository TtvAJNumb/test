package com.donututils.realworld.dynamicshop.storage;

import com.donututils.realworld.dynamicshop.model.Loan;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LoanStore {

    private final File file;
    private final Logger logger;

    public LoanStore(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "loans.yml");
        this.logger = logger;
    }

    public Map<UUID, Loan> load() {
        Map<UUID, Loan> loans = new LinkedHashMap<>();
        if (!file.exists()) {
            return loans;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("loans");
        if (section == null) {
            return loans;
        }
        for (String key : section.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) {
                continue;
            }
            long issuedAt = s.getLong("issuedAtMillis", System.currentTimeMillis());
            Loan loan = new Loan(s.getString("currency", "money"), s.getDouble("principal", 0),
                    s.getDouble("dailyInterestRatePercent", 5.0), issuedAt);
            loan.setLastAccrualMillis(s.getLong("lastAccrualMillis", issuedAt));
            if (!loan.isPaidOff()) {
                loans.put(playerId, loan);
            }
        }
        return loans;
    }

    public synchronized void save(Map<UUID, Loan> loans) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Loan> entry : loans.entrySet()) {
            String path = "loans." + entry.getKey();
            Loan loan = entry.getValue();
            yaml.set(path + ".currency", loan.currency());
            yaml.set(path + ".principal", loan.principal());
            yaml.set(path + ".dailyInterestRatePercent", loan.dailyInterestRatePercent());
            yaml.set(path + ".issuedAtMillis", loan.issuedAtMillis());
            yaml.set(path + ".lastAccrualMillis", loan.lastAccrualMillis());
        }
        try {
            AtomicFiles.writeYaml(yaml, file);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to save loans.yml", ex);
        }
    }
}
