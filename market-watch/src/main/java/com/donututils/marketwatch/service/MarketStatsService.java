package com.donututils.marketwatch.service;

import com.donututils.marketwatch.model.AuctionSaleRecord;
import com.donututils.marketwatch.model.EconomySnapshot;
import com.donututils.marketwatch.storage.AuctionSaleStore;
import com.donututils.marketwatch.storage.EconomySnapshotStore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MarketStatsService {

    private final EconomySnapshotStore economyStore;
    private final AuctionSaleStore auctionStore;

    public MarketStatsService(EconomySnapshotStore economyStore, AuctionSaleStore auctionStore) {
        this.economyStore = economyStore;
        this.auctionStore = auctionStore;
    }

    public record ItemStats(String material, double avgPrice, double minPrice, double maxPrice, int volume) {
    }

    /** Aggregates every sale of the given material recorded in the last {@code windowMillis}. */
    public ItemStats getItemStats(String material, long windowMillis) {
        long cutoff = System.currentTimeMillis() - windowMillis;
        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = 0;
        int volume = 0;
        for (AuctionSaleRecord record : auctionStore.readAll()) {
            if (!record.material().equalsIgnoreCase(material) || record.soldAt() < cutoff) {
                continue;
            }
            sum += record.price();
            min = Math.min(min, record.price());
            max = Math.max(max, record.price());
            volume++;
        }
        if (volume == 0) {
            return new ItemStats(material, 0, 0, 0, 0);
        }
        return new ItemStats(material, sum / volume, min, max, volume);
    }

    /** Most-traded materials in the last {@code windowMillis}, highest volume first. */
    public List<ItemStats> getTopTradedItems(long windowMillis, int limit) {
        long cutoff = System.currentTimeMillis() - windowMillis;
        Map<String, List<Double>> byMaterial = new LinkedHashMap<>();
        for (AuctionSaleRecord record : auctionStore.readAll()) {
            if (record.soldAt() < cutoff) {
                continue;
            }
            byMaterial.computeIfAbsent(record.material(), key -> new ArrayList<>()).add(record.price());
        }

        List<ItemStats> stats = new ArrayList<>();
        for (Map.Entry<String, List<Double>> entry : byMaterial.entrySet()) {
            List<Double> prices = entry.getValue();
            double sum = 0;
            double min = Double.MAX_VALUE;
            double max = 0;
            for (double price : prices) {
                sum += price;
                min = Math.min(min, price);
                max = Math.max(max, price);
            }
            stats.add(new ItemStats(entry.getKey(), sum / prices.size(), min, max, prices.size()));
        }

        stats.sort(Comparator.comparingInt(ItemStats::volume).reversed());
        return stats.size() > limit ? stats.subList(0, limit) : stats;
    }

    public record EconomyChange(double currentTotal, double previousTotal, double percentChange, int playerCount) {
    }

    /** Compares the latest snapshot to the closest one at or before {@code windowMillis} ago. */
    public EconomyChange getEconomyChange(long windowMillis) {
        List<EconomySnapshot> snapshots = economyStore.readAll();
        if (snapshots.isEmpty()) {
            return new EconomyChange(0, 0, 0, 0);
        }
        EconomySnapshot latest = snapshots.get(snapshots.size() - 1);
        long targetTime = latest.timestampMillis() - windowMillis;

        EconomySnapshot closest = snapshots.get(0);
        for (EconomySnapshot snapshot : snapshots) {
            if (snapshot.timestampMillis() <= targetTime) {
                closest = snapshot;
            }
        }

        double percentChange = closest.totalMoney() == 0 ? 0
                : ((latest.totalMoney() - closest.totalMoney()) / closest.totalMoney()) * 100.0;
        return new EconomyChange(latest.totalMoney(), closest.totalMoney(), percentChange, latest.playerCount());
    }
}
