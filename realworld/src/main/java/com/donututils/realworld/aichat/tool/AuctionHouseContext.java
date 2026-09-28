package com.donututils.realworld.aichat.tool;

import com.donututils.realworld.aichat.json.MiniJson;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Read-only lookups against MarketWatch's auction-sales.log ({@code listingId|material|price|category|soldAt}
 * lines). No compile-time dependency on that plugin - just reads its public, non-sensitive sale
 * history straight off disk. This reflects completed sales, not live shop listings.
 */
final class AuctionHouseContext {

    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    private AuctionHouseContext() {
    }

    private record Sale(String material, double price, long soldAt) {
    }

    @SuppressWarnings("unchecked")
    static void registerTools(ServerContextService service) {
        service.register(new ToolDefinition(
                "auction_house_item_stats",
                (Map<String, Object>) MiniJson.parse("""
                        {"name":"auction_house_item_stats","description":"Get recent auction house sale stats (min/avg/max price, number sold) for one item material.","input_schema":{"type":"object","properties":{"item":{"type":"string","description":"Item material name, e.g. DIAMOND"},"days":{"type":"number","description":"How many days back to look, defaults to 7"}},"required":["item"]}}
                        """),
                (args, sender) -> itemStats(args.get("item"), parseDays(args.get("days")))
        ));
        service.register(new ToolDefinition(
                "auction_house_top_traded",
                (Map<String, Object>) MiniJson.parse("""
                        {"name":"auction_house_top_traded","description":"List the most-traded auction house items recently with their average and cheapest recorded sale price. Useful for 'what's cheapest on the AH' or 'what's popular' style questions.","input_schema":{"type":"object","properties":{"days":{"type":"number","description":"How many days back to look, defaults to 7"}}}}
                        """),
                (args, sender) -> topTraded(parseDays(args.get("days")))
        ));
    }

    private static int parseDays(String raw) {
        if (raw == null || raw.isBlank()) {
            return 7;
        }
        try {
            int days = (int) Math.round(Double.parseDouble(raw));
            return Math.max(1, Math.min(days, 90));
        } catch (NumberFormatException ex) {
            return 7;
        }
    }

    private static File salesFile() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("MarketWatch");
        if (plugin == null) {
            return null;
        }
        return new File(plugin.getDataFolder(), "auction-sales.log");
    }

    private static List<Sale> readSales() {
        List<Sale> sales = new ArrayList<>();
        File file = salesFile();
        if (file == null || !file.exists()) {
            return sales;
        }
        try {
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                String[] parts = line.split("\\|", -1);
                if (parts.length != 5) {
                    continue;
                }
                try {
                    sales.add(new Sale(parts[1], Double.parseDouble(parts[2]), Long.parseLong(parts[4])));
                } catch (NumberFormatException ignored) {
                    // skip malformed line
                }
            }
        } catch (java.io.IOException ignored) {
            // no data available
        }
        return sales;
    }

    private static String itemStats(String item, int days) {
        if (item == null || item.isBlank()) {
            return "No item was given.";
        }
        String material = item.toUpperCase(Locale.ROOT);
        long cutoff = System.currentTimeMillis() - (days * DAY_MILLIS);
        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = 0;
        int volume = 0;
        for (Sale sale : readSales()) {
            if (!sale.material().equalsIgnoreCase(material) || sale.soldAt() < cutoff) {
                continue;
            }
            sum += sale.price();
            min = Math.min(min, sale.price());
            max = Math.max(max, sale.price());
            volume++;
        }
        if (volume == 0) {
            return "No recorded auction house sales of " + material + " in the last " + days + " day(s).";
        }
        return String.format(Locale.US,
                "%s over the last %d day(s): %d sold, avg $%,.2f, cheapest $%,.2f, most expensive $%,.2f.",
                material, days, volume, sum / volume, min, max);
    }

    private static String topTraded(int days) {
        long cutoff = System.currentTimeMillis() - (days * DAY_MILLIS);
        Map<String, double[]> byMaterial = new LinkedHashMap<>();
        for (Sale sale : readSales()) {
            if (sale.soldAt() < cutoff) {
                continue;
            }
            double[] agg = byMaterial.computeIfAbsent(sale.material(), key -> new double[]{0, 0, Double.MAX_VALUE});
            agg[0] += sale.price();
            agg[1] += 1;
            agg[2] = Math.min(agg[2], sale.price());
        }
        if (byMaterial.isEmpty()) {
            return "No auction house sales recorded in the last " + days + " day(s).";
        }
        List<Map.Entry<String, double[]>> entries = new ArrayList<>(byMaterial.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue()[1], a.getValue()[1]));
        StringBuilder out = new StringBuilder("Most-traded items in the last " + days + " day(s): ");
        int limit = Math.min(10, entries.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, double[]> entry = entries.get(i);
            double[] agg = entry.getValue();
            int volume = (int) agg[1];
            double avg = agg[0] / volume;
            if (i > 0) {
                out.append("; ");
            }
            out.append(entry.getKey()).append(" (avg $").append(String.format(Locale.US, "%,.2f", avg))
                    .append(", cheapest $").append(String.format(Locale.US, "%,.2f", agg[2]))
                    .append(", ").append(volume).append(" sold)");
        }
        return out.toString();
    }
}
