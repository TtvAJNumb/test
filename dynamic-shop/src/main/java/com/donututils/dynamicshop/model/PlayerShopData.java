package com.donututils.dynamicshop.model;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Per-player shop state: which materials they've unlocked for purchase (mined/sold at least once,
 * used by the optional purchase-restriction feature) and which materials they've turned auto-sell
 * on for. */
public final class PlayerShopData {

    private final Set<String> unlockedMaterials = ConcurrentHashMap.newKeySet();
    private final Set<String> autoSellMaterials = ConcurrentHashMap.newKeySet();
    private volatile boolean tutorialSeen;

    public boolean hasUnlocked(String material) {
        return unlockedMaterials.contains(material.toUpperCase());
    }

    public void unlock(String material) {
        unlockedMaterials.add(material.toUpperCase());
    }

    public Set<String> unlockedMaterials() {
        return unlockedMaterials;
    }

    public boolean isAutoSellEnabled(String material) {
        return autoSellMaterials.contains(material.toUpperCase());
    }

    public void setAutoSell(String material, boolean enabled) {
        String key = material.toUpperCase();
        if (enabled) {
            autoSellMaterials.add(key);
        } else {
            autoSellMaterials.remove(key);
        }
    }

    public Set<String> autoSellMaterials() {
        return autoSellMaterials;
    }

    public boolean tutorialSeen() {
        return tutorialSeen;
    }

    public void setTutorialSeen(boolean tutorialSeen) {
        this.tutorialSeen = tutorialSeen;
    }
}
