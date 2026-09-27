package com.donututils.dynamicshop.engine;

import com.donututils.dynamicshop.model.PlayerShopData;
import com.donututils.dynamicshop.storage.PlayerDataStore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDataRegistry {

    private final PlayerDataStore store;
    private final Map<UUID, PlayerShopData> data;

    public PlayerDataRegistry(PlayerDataStore store) {
        this.store = store;
        this.data = new ConcurrentHashMap<>(store.load());
    }

    public PlayerShopData get(UUID playerId) {
        return data.computeIfAbsent(playerId, id -> new PlayerShopData());
    }

    public void saveAll() {
        store.save(data);
    }
}
