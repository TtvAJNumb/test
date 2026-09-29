package com.donututils.realworld.punishhistory;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class NoteCommand implements CommandExecutor {

    private final NoteStore noteStore;

    public NoteCommand(NoteStore noteStore) {
        this.noteStore = noteStore;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /note <add|remove|list> <player> [text|index]"));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> {
                if (!sender.hasPermission("punishmenthistorygui.notes.add")) {
                    sender.sendMessage(color("&cYou do not have permission to do that."));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(color("&cUsage: /note add <player> <text>"));
                    return true;
                }
                String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                noteStore.add(target.getUniqueId(), sender.getName(), text);
                sender.sendMessage(color("&aNote added to " + args[1] + "."));
            }
            case "remove" -> {
                if (!sender.hasPermission("punishmenthistorygui.notes.remove")) {
                    sender.sendMessage(color("&cYou do not have permission to do that."));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(color("&cUsage: /note remove <player> <index>"));
                    return true;
                }
                try {
                    int index = Integer.parseInt(args[2]);
                    if (noteStore.removeByIndex(target.getUniqueId(), index)) {
                        sender.sendMessage(color("&aRemoved note #" + index + " from " + args[1] + "."));
                    } else {
                        sender.sendMessage(color("&cNo note #" + index + " on " + args[1] + "."));
                    }
                } catch (NumberFormatException ex) {
                    sender.sendMessage(color("&cInvalid note index."));
                }
            }
            case "list" -> {
                if (!sender.hasPermission("punishmenthistorygui.notes.view")) {
                    sender.sendMessage(color("&cYou do not have permission to do that."));
                    return true;
                }
                List<NoteStore.Note> notes = noteStore.notesFor(target.getUniqueId());
                sender.sendMessage(color("&6&l" + args[1] + "'s Notes"));
                if (notes.isEmpty()) {
                    sender.sendMessage(color("&7None."));
                } else {
                    int index = 1;
                    for (NoteStore.Note note : notes) {
                        sender.sendMessage(color("&e#" + index + " &7by " + note.authorName() + ": &f" + note.text()));
                        index++;
                    }
                }
            }
            default -> sender.sendMessage(color("&cUsage: /note <add|remove|list> <player> [text|index]"));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
