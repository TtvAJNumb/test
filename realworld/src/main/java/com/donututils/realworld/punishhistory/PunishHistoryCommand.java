package com.donututils.realworld.punishhistory;

import com.donututils.realworld.municipal.court.CourtManager;
import com.donututils.realworld.municipal.model.CourtCase;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * Rebuilt against Municipal's own CourtManager rather than UltimateDonutSmp's PunishmentManager -
 * the closest real analog RealWorld has (a permanent per-player record of arrests/convictions/
 * dismissals). Chat-printed rather than the original's paginated GUI, to keep this pass's scope
 * bounded; the /note staff-notes half is unchanged in spirit (a small flat record per player) and is
 * still backed by its own store.
 */
public final class PunishHistoryCommand implements CommandExecutor {

    private final CourtManager courtManager;
    private final NoteStore noteStore;

    public PunishHistoryCommand(CourtManager courtManager, NoteStore noteStore) {
        this.courtManager = courtManager;
        this.noteStore = noteStore;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        OfflinePlayer target;
        if (args.length >= 1) {
            target = Bukkit.getOfflinePlayer(args[0]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(color("&cUsage: /punishhistory <player>"));
            return true;
        }

        sender.sendMessage(color("&6&l" + target.getName() + "'s History"));
        List<CourtCase> cases = courtManager.recordFor(target.getUniqueId());
        if (cases.isEmpty()) {
            sender.sendMessage(color("&7Clean record."));
        } else {
            for (CourtCase courtCase : cases) {
                String statusColor = switch (courtCase.status()) {
                    case CONVICTED -> "&c";
                    case DISMISSED -> "&7";
                    case PENDING -> "&e";
                };
                sender.sendMessage(color(statusColor + "#" + courtCase.id() + " " + courtCase.status() + " &7- " + courtCase.reason()
                        + (courtCase.status() == CourtCase.Status.CONVICTED ? " (" + courtCase.sentenceMinutes() + "m)" : "")));
            }
        }

        List<NoteStore.Note> notes = noteStore.notesFor(target.getUniqueId());
        if (!notes.isEmpty()) {
            sender.sendMessage(color("&6&lStaff Notes"));
            int index = 1;
            for (NoteStore.Note note : notes) {
                sender.sendMessage(color("&e#" + index + " &7by " + note.authorName() + ": &f" + note.text()));
                index++;
            }
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
