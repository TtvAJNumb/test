package com.donututils.donutrep.staff;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** /staffmode - a one-command bundle of "go investigate something": saves the player's survival
 * inventory and game mode, switches them to creative + vanish + flight with an empty inventory, then
 * restores everything exactly as it was on toggle-off. */
public final class StaffModeManager {

    private record SavedState(ItemStack[] contents, GameMode gameMode, boolean wasFlying, boolean hadAllowFlight) {
    }

    private final VanishManager vanishManager;
    private final Map<UUID, SavedState> saved = new HashMap<>();

    public StaffModeManager(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    public boolean isInStaffMode(UUID playerId) {
        return saved.containsKey(playerId);
    }

    /** Returns true if staff mode is now ON for this player. */
    public boolean toggle(Player player) {
        UUID id = player.getUniqueId();
        SavedState previous = saved.remove(id);
        if (previous != null) {
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                player.getInventory().setItem(i, null);
            }
            for (int i = 0; i < previous.contents().length && i < player.getInventory().getSize(); i++) {
                player.getInventory().setItem(i, previous.contents()[i]);
            }
            player.setGameMode(previous.gameMode());
            player.setAllowFlight(previous.hadAllowFlight());
            player.setFlying(previous.wasFlying());
            if (vanishManager.isVanished(id)) {
                vanishManager.toggle(player);
            }
            return false;
        }

        ItemStack[] contents = player.getInventory().getContents();
        ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            copy[i] = contents[i] != null ? contents[i].clone() : null;
        }
        saved.put(id, new SavedState(copy, player.getGameMode(), player.isFlying(), player.getAllowFlight()));

        for (int i = 0; i < contents.length; i++) {
            player.getInventory().setItem(i, null);
        }
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);
        if (!vanishManager.isVanished(id)) {
            vanishManager.toggle(player);
        }
        return true;
    }
}
