package com.donututils.punishhistory.gui;

import com.donututils.punishhistory.config.PunishConfig;
import com.donututils.punishhistory.model.RosterEntry;
import com.donututils.punishhistory.notes.NoteStore;
import com.donututils.punishhistory.reflect.UdsBridge;
import com.donututils.punishhistory.roster.RosterBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** The landing menu: every player with recent punishments or staff notes, newest first. */
public final class RosterMenu {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter
            .ofPattern("MMM d, HH:mm", Locale.US)
            .withZone(ZoneId.systemDefault());

    private RosterMenu() {
    }

    public static void open(Player player, UdsBridge bridge, NoteStore noteStore, PunishConfig config)
            throws ReflectiveOperationException {
        List<RosterEntry> roster = RosterBuilder.build(bridge, noteStore, config.rosterScanSize());

        List<ItemStack> items = new ArrayList<>();
        List<Consumer<Player>> actions = new ArrayList<>();
        for (RosterEntry entry : roster) {
            items.add(buildHead(entry));
            actions.add(viewer -> {
                try {
                    DetailMenu.open(viewer, bridge, noteStore, config, entry.uuid(), entry.name());
                } catch (ReflectiveOperationException ex) {
                    viewer.sendMessage(PagedMenu.legacy("&cFailed to load that player's history. Check console."));
                }
            });
        }
        if (items.isEmpty()) {
            items.add(emptyItem());
            actions.add(null);
        }

        PunishMenuHolder holder = new PunishMenuHolder();
        PagedMenu.open(player, holder, config.rosterTitle(), items, actions, null);
    }

    private static ItemStack buildHead(RosterEntry entry) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = head.getItemMeta();
        if (rawMeta instanceof SkullMeta meta) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.uuid()));
            meta.setDisplayName(PagedMenu.legacy("&f" + entry.name()));

            List<String> lore = new ArrayList<>();
            lore.add(PagedMenu.legacy("&ePunishments (recent window): &f" + entry.punishmentCount()));
            lore.add(PagedMenu.legacy("&eStaff notes: &f" + entry.noteCount()));
            if (entry.lastActivity() > 0) {
                lore.add(PagedMenu.legacy("&7Last activity: &f" + FORMAT.format(Instant.ofEpochMilli(entry.lastActivity()))));
            }
            lore.add(PagedMenu.legacy(" "));
            lore.add(PagedMenu.legacy("&aClick to view full history"));
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    private static ItemStack emptyItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(PagedMenu.legacy("&aNo punishment history or staff notes yet"));
            item.setItemMeta(meta);
        }
        return item;
    }
}
