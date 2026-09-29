package com.donututils.donutrep.punishhistory;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Staff notes history for a player. DonutREP has no court/police system (dropped along with
 * Municipal), so this shows the staff-notes record only rather than a criminal record.
 */
public final class PunishHistoryCommand implements CommandExecutor {

    private final NoteStore noteStore;

    public PunishHistoryCommand(NoteStore noteStore) {
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

        sender.sendMessage(color("&6&l" + target.getName() + "'s Staff Notes"));
        List<NoteStore.Note> notes = noteStore.notesFor(target.getUniqueId());
        if (notes.isEmpty()) {
            sender.sendMessage(color("&7No notes on file."));
        } else {
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
