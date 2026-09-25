package com.donututils.punishhistory.gui;

import com.donututils.punishhistory.config.PunishConfig;
import com.donututils.punishhistory.model.Note;
import com.donututils.punishhistory.model.PunishmentSnapshot;
import com.donututils.punishhistory.notes.NoteStore;
import com.donututils.punishhistory.reflect.UdsBridge;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/** One player's combined bans/mutes/warns/kicks/blacklists and staff notes, newest first. */
public final class DetailMenu {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter
            .ofPattern("MMM d, yyyy HH:mm", Locale.US)
            .withZone(ZoneId.systemDefault());

    private DetailMenu() {
    }

    public static void open(Player player, UdsBridge bridge, NoteStore noteStore, PunishConfig config,
                             UUID targetUuid, String targetName) throws ReflectiveOperationException {
        List<AbstractMap.SimpleEntry<Long, ItemStack>> rows = new ArrayList<>();

        for (PunishmentSnapshot snapshot : bridge.getHistory(targetUuid, targetName, 500, 0)) {
            rows.add(new AbstractMap.SimpleEntry<>(snapshot.issuedAt(), buildPunishmentItem(snapshot)));
        }
        for (Note note : noteStore.getNotes(targetUuid)) {
            rows.add(new AbstractMap.SimpleEntry<>(note.timestamp(), buildNoteItem(note)));
        }
        rows.sort(Comparator.<AbstractMap.SimpleEntry<Long, ItemStack>>comparingLong(AbstractMap.SimpleEntry::getKey).reversed());

        List<ItemStack> items = new ArrayList<>();
        for (AbstractMap.SimpleEntry<Long, ItemStack> row : rows) {
            items.add(row.getValue());
        }
        if (items.isEmpty()) {
            items.add(emptyItem());
        }

        List<Consumer<Player>> actions = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            actions.add(null);
        }

        Consumer<Player> back = viewer -> {
            try {
                RosterMenu.open(viewer, bridge, noteStore, config);
            } catch (ReflectiveOperationException ex) {
                viewer.sendMessage(PagedMenu.legacy("&cFailed to reload the roster. Check console."));
            }
        };

        PunishMenuHolder holder = new PunishMenuHolder();
        String title = config.detailTitleTemplate().replace("%player%", targetName);
        PagedMenu.open(player, holder, title, items, actions, back);
    }

    private static ItemStack buildPunishmentItem(PunishmentSnapshot snapshot) {
        ItemStack item = new ItemStack(iconFor(snapshot.type()));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(PagedMenu.legacy(colorFor(snapshot.type()) + snapshot.displayType()));

            List<String> lore = new ArrayList<>();
            lore.add(PagedMenu.legacy("&eReason: &f" + snapshot.reason()));
            lore.add(PagedMenu.legacy("&eBy: &f" + snapshot.issuerName()));
            lore.add(PagedMenu.legacy("&eState: &f" + snapshot.state()));
            if (snapshot.expiresAt() != null && snapshot.expiresAt() > 0) {
                lore.add(PagedMenu.legacy("&eExpires: &f" + FORMAT.format(Instant.ofEpochMilli(snapshot.expiresAt()))));
            }
            if (snapshot.removed()) {
                lore.add(PagedMenu.legacy("&eRemoved by: &f" + snapshot.removedByName()));
                lore.add(PagedMenu.legacy("&eRemoval reason: &f" + snapshot.removalReason()));
            }
            lore.add(PagedMenu.legacy("&7" + FORMAT.format(Instant.ofEpochMilli(snapshot.issuedAt()))));
            meta.setLore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildNoteItem(Note note) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(PagedMenu.legacy("&bStaff Note"));

            List<String> lore = new ArrayList<>();
            lore.add(PagedMenu.legacy("&f" + note.text()));
            lore.add(PagedMenu.legacy("&eBy: &f" + note.authorName()));
            lore.add(PagedMenu.legacy("&7" + FORMAT.format(Instant.ofEpochMilli(note.timestamp()))));
            meta.setLore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack emptyItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(PagedMenu.legacy("&aNo punishments or notes on file"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static Material iconFor(String type) {
        return switch (type) {
            case "BAN" -> Material.RED_CONCRETE;
            case "MUTE", "VOICE_MUTE" -> Material.ORANGE_CONCRETE;
            case "WARN" -> Material.YELLOW_CONCRETE;
            case "KICK" -> Material.GRAY_CONCRETE;
            case "BLACKLIST" -> Material.BLACK_CONCRETE;
            default -> Material.STONE;
        };
    }

    private static String colorFor(String type) {
        return switch (type) {
            case "BAN" -> "&c";
            case "MUTE", "VOICE_MUTE" -> "&6";
            case "WARN" -> "&e";
            case "KICK" -> "&7";
            case "BLACKLIST" -> "&8";
            default -> "&f";
        };
    }
}
