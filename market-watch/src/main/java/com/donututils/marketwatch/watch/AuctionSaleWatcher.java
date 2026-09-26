package com.donututils.marketwatch.watch;

import com.donututils.marketwatch.config.MarketWatchConfig;
import com.donututils.marketwatch.model.AuctionSaleRecord;
import com.donututils.marketwatch.reflect.UdsBridge;
import com.donututils.marketwatch.storage.AuctionSaleStore;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * UDS exposes active listings but no "recently sold" feed, so this watches the active set and
 * notices when a listing id it previously saw drops out of it - at that point {@code getListing}
 * (which works regardless of status) says whether it sold, expired, or was cancelled.
 */
public final class AuctionSaleWatcher implements Runnable {

    private final Plugin plugin;
    private final UdsBridge bridge;
    private final AuctionSaleStore store;
    private final Supplier<MarketWatchConfig> configSupplier;
    private Set<Long> lastSeenActiveIds = null;

    public AuctionSaleWatcher(Plugin plugin, UdsBridge bridge, AuctionSaleStore store, Supplier<MarketWatchConfig> configSupplier) {
        this.plugin = plugin;
        this.bridge = bridge;
        this.store = store;
        this.configSupplier = configSupplier;
    }

    @Override
    public void run() {
        try {
            Set<Long> currentActiveIds = new HashSet<>();
            for (UdsBridge.ActiveListing listing : bridge.getActiveListings()) {
                currentActiveIds.add(listing.id());
            }

            if (lastSeenActiveIds != null) {
                for (Long id : lastSeenActiveIds) {
                    if (currentActiveIds.contains(id)) {
                        continue;
                    }
                    handleDroppedListing(id);
                }
            }

            lastSeenActiveIds = currentActiveIds;
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to poll the auction house", ex);
        }
    }

    private void handleDroppedListing(long id) {
        UdsBridge.ActiveListing finalState = bridge.getListingById(id);
        if (finalState == null || !"SOLD".equals(finalState.status())) {
            return;
        }
        if (!configSupplier.get().isMaterialTracked(finalState.material())) {
            return;
        }
        store.append(new AuctionSaleRecord(
                finalState.id(),
                finalState.material(),
                finalState.price(),
                finalState.category(),
                finalState.soldAt() > 0 ? finalState.soldAt() : System.currentTimeMillis()
        ));
    }
}
