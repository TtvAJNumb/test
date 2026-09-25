package com.donututils.punishhistory.command;

import com.donututils.punishhistory.model.Note;
import com.donututils.punishhistory.notes.NoteStore;
import com.donututils.punishhistory.reflect.UdsBridge;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class NoteCommand implements CommandExecutor, TabCompleter {

    private final NoteStore noteStore;
    private final UdsBridge bridge;

    public NoteCommand(NoteStore noteStore, UdsBridge bridge) {
        this.noteStore = noteStore;
        this.bridge = bridge;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(legacy("&cUsage: /note <add|remove|list> <player> [text|index]"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        String targetQuery = args[1];

        UUID targetUuid;
        String targetName;
        Optional<UUID> resolved = bridge.resolveTargetUuid(targetQuery);
        if (resolved.isPresent()) {
            targetUuid = resolved.get();
            targetName = bridge.resolveTargetName(targetUuid);
        } else {
            @SuppressWarnings("deprecation")
            OfflinePlayer offline = Bukkit.getOfflinePlayer(targetQuery);
            if (!offline.hasPlayedBefore() && offline.getName() == null) {
                sender.sendMessage(legacy("&cUnknown player &f" + targetQuery + "&c."));
                return true;
            }
            targetUuid = offline.getUniqueId();
            targetName = offline.getName() != null ? offline.getName() : targetQuery;
        }

        switch (sub) {
            case "add" -> handleAdd(sender, args, targetUuid, targetName);
            case "remove" -> handleRemove(sender, args, targetUuid, targetName);
            case "list" -> handleList(sender, targetUuid, targetName);
            default -> sender.sendMessage(legacy("&cUsage: /note <add|remove|list> <player> [text|index]"));
        }
        return true;
    }

    private void handleAdd(CommandSender sender, String[] args, UUID targetUuid, String targetName) {
        if (!sender.hasPermission("punishmenthistorygui.notes.add")) {
            sender.sendMessage(legacy("&cYou do not have permission to add notes."));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(legacy("&cUsage: /note add <player> <text>"));
            return;
        }
        String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        String authorName = sender instanceof Player p ? p.getName() : "console";
        noteStore.addNote(targetUuid, new Note(System.currentTimeMillis(), authorName, text));
        sender.sendMessage(legacy("&aAdded a note to &f" + targetName + "&a."));
    }

    private void handleRemove(CommandSender sender, String[] args, UUID targetUuid, String targetName) {
        if (!sender.hasPermission("punishmenthistorygui.notes.remove")) {
            sender.sendMessage(legacy("&cYou do not have permission to remove notes."));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(legacy("&cUsage: /note remove <player> <index>"));
            return;
        }
        int index;
        try {
            index = Integer.parseInt(args[2]) - 1;
        } catch (NumberFormatException ex) {
            sender.sendMessage(legacy("&cIndex must be a number. Use /note list <player> to see indexes."));
            return;
        }
        boolean removed = noteStore.removeNote(targetUuid, index);
        sender.sendMessage(removed
                ? legacy("&aRemoved note #" + (index + 1) + " from &f" + targetName + "&a.")
                : legacy("&cNo note #" + (index + 1) + " on &f" + targetName + "&c."));
    }

    private void handleList(CommandSender sender, UUID targetUuid, String targetName) {
        if (!sender.hasPermission("punishmenthistorygui.notes.view")) {
            sender.sendMessage(legacy("&cYou do not have permission to view notes."));
            return;
        }
        List<Note> notes = noteStore.getNotes(targetUuid);
        if (notes.isEmpty()) {
            sender.sendMessage(legacy("&7" + targetName + " has no staff notes."));
            return;
        }
        sender.sendMessage(legacy("&5Notes for &f" + targetName + "&5:"));
        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            sender.sendMessage(legacy("&7#" + (i + 1) + " &f" + note.text() + " &7(" + note.authorName() + ")"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(List.of("add", "remove", "list"), args[0]);
        }
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return filterStartsWith(names, args[1]);
        }
        return List.of();
    }

    private static List<String> filterStartsWith(List<String> options, String partial) {
        String lower = partial.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(lower)).collect(Collectors.toList());
    }

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
