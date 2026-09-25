package com.donututils.crateodds.command;

import com.donututils.crateodds.gui.HistoryMenu;
import com.donututils.crateodds.history.HistoryStore;
import com.donututils.crateodds.model.HistoryEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public final class CrateHistoryCommand implements CommandExecutor, TabCompleter {

    private final HistoryStore historyStore;
    private final String titleTemplate;

    public CrateHistoryCommand(HistoryStore historyStore, String titleTemplate) {
        this.historyStore = historyStore;
        this.titleTemplate = titleTemplate;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        UUID targetUuid;
        String targetName;
        if (args.length >= 1) {
            if (!viewer.hasPermission("crateoddshistory.history.others")) {
                viewer.sendMessage(legacy("&cYou do not have permission to view other players' crate history."));
                return true;
            }
            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target.getName() == null && !target.hasPlayedBefore()) {
                viewer.sendMessage(legacy("&cUnknown player &f" + args[0] + "&c."));
                return true;
            }
            targetUuid = target.getUniqueId();
            targetName = target.getName() != null ? target.getName() : args[0];
        } else {
            targetUuid = viewer.getUniqueId();
            targetName = viewer.getName();
        }

        List<HistoryEntry> entries = historyStore.getEntries(targetUuid);
        HistoryMenu.open(viewer, titleTemplate, targetName, entries);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("crateoddshistory.history.others")) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(partial))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
