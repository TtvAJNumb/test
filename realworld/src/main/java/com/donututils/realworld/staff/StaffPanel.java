package com.donututils.realworld.staff;

import com.donututils.realworld.staff.gui.StaffMenuHolder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * The `/sus` staff panel: a roster of online players, and a per-player inspection screen with
 * freeze/invsee/endersee actions. A from-scratch native replacement for what SusPlayerFinder used to
 * bridge into the real UltimateDonutSmp for (freeze manager, invsee manager, ender chest manager).
 * GrimAC cheat-flag correlation from the original tool is deliberately not rebuilt here yet - that's
 * a live-event integration with a third-party anticheat, kept out of this pass's scope.
 */
public final class StaffPanel {

    private static final int SIZE = 54;

    private final FreezeManager freezeManager;

    public StaffPanel(FreezeManager freezeManager) {
        this.freezeManager = freezeManager;
    }

    public void openRoster(Player viewer) {
        StaffMenuHolder holder = new StaffMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, SIZE, color("&6&lOnline Players"));
        holder.setInventory(inventory);

        List<ItemStack> items = new ArrayList<>();
        List<java.util.function.Consumer<Player>> actions = new ArrayList<>();
        for (Player target : Bukkit.getOnlinePlayers()) {
            items.add(playerHead(target, freezeManager.isFrozen(target.getUniqueId())));
            actions.add(p -> openInspection(p, target));
            if (items.size() >= SIZE) {
                break;
            }
        }
        holder.setItems(items);
        holder.setActions(actions);
        for (int i = 0; i < items.size(); i++) {
            inventory.setItem(i, items.get(i));
        }
        openInventorySafely(viewer, inventory);
    }

    public void openInspection(Player viewer, Player target) {
        StaffMenuHolder holder = new StaffMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, color("&6&lInspecting " + target.getName()));
        holder.setInventory(inventory);

        List<ItemStack> items = new ArrayList<>(java.util.Collections.nCopies(27, null));
        List<java.util.function.Consumer<Player>> actions = new ArrayList<>(java.util.Collections.nCopies(27, null));

        boolean frozen = freezeManager.isFrozen(target.getUniqueId());
        items.set(11, icon(frozen ? Material.ICE : Material.WATER_BUCKET,
                (frozen ? "&b&lUnfreeze " : "&b&lFreeze ") + target.getName(),
                List.of("&7Click to " + (frozen ? "let them move again." : "lock them in place."))));
        actions.set(11, p -> {
            if (!p.hasPermission("staff.freeze")) {
                p.sendMessage(color("&cYou do not have permission to do that."));
                return;
            }
            if (target.hasPermission("staff.freezeexempt")) {
                p.sendMessage(color("&c" + target.getName() + " is exempt from freezing."));
                return;
            }
            boolean nowFrozen = freezeManager.toggle(target.getUniqueId());
            p.sendMessage(color((nowFrozen ? "&aFroze " : "&aUnfroze ") + target.getName() + "&a."));
            target.sendMessage(color(nowFrozen ? "&cYou have been frozen by staff." : "&aYou've been unfrozen."));
            openInspection(p, target);
        });

        items.set(13, icon(Material.CHEST, "&e&lView Inventory", List.of("&7Live view - editable.")));
        actions.set(13, p -> {
            if (!p.hasPermission("staff.invsee")) {
                p.sendMessage(color("&cYou do not have permission to do that."));
                return;
            }
            openInventorySafely(p, target.getInventory());
        });

        items.set(15, icon(Material.ENDER_CHEST, "&5&lView Ender Chest", List.of("&7Live view - editable.")));
        actions.set(15, p -> {
            if (!p.hasPermission("staff.endersee")) {
                p.sendMessage(color("&cYou do not have permission to do that."));
                return;
            }
            openInventorySafely(p, target.getEnderChest());
        });

        items.set(22, icon(Material.ARROW, "&a&lBack to Roster", List.of()));
        actions.set(22, this::openRoster);

        holder.setItems(items);
        holder.setActions(actions);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) != null) {
                inventory.setItem(i, items.get(i));
            }
        }
        openInventorySafely(viewer, inventory);
    }

    /** See OnboardingGuiService's note on why this is reflective rather than a direct call. */
    private static void openInventorySafely(Player player, Inventory inventory) {
        try {
            Method method = player.getClass().getMethod("openInventory", Inventory.class);
            method.invoke(player, inventory);
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            throw new IllegalStateException("Could not find a way to open an inventory on this server: " + ex, ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new IllegalStateException("Opening the menu inventory failed: " + cause, cause);
        }
    }

    private static ItemStack playerHead(Player target, boolean frozen) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(target);
            item.setItemMeta(skullMeta);
        }
        if (meta != null) {
            meta.setDisplayName(color((frozen ? "&b&l❄ " : "&f") + target.getName()));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            if (!lore.isEmpty()) {
                meta.setLore(lore.stream().map(StaffPanel::color).toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
