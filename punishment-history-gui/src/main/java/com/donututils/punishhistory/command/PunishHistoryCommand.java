package com.donututils.punishhistory.command;

import com.donututils.punishhistory.config.PunishConfig;
import com.donututils.punishhistory.gui.DetailMenu;
import com.donututils.punishhistory.gui.PagedMenu;
import com.donututils.punishhistory.gui.RosterMenu;
import com.donututils.punishhistory.notes.NoteStore;
import com.donututils.punishhistory.reflect.UdsBridge;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class PunishHistoryCommand implements CommandExecutor, TabCompleter {

    private final UdsBridge bridge;
    private final NoteStore noteStore;
    private final PunishConfig config;

    public PunishHistoryCommand(UdsBridge bridge, NoteStore noteStore, PunishConfig config) {
        this.bridge = bridge;
        this.noteStore = noteStore;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        try {
            if (args.length == 0) {
                RosterMenu.open(player, bridge, noteStore, config);
                return true;
            }

            String query = args[0];
            Optional<UUID> resolved = bridge.resolveTargetUuid(query);
            if (resolved.isEmpty()) {
                player.sendMessage(PagedMenu.legacy("&cCould not find a player or punishment record for &f" + query + "&c."));
                return true;
            }
            UUID targetUuid = resolved.get();
            String targetName = bridge.resolveTargetName(targetUuid);
            DetailMenu.open(player, bridge, noteStore, config, targetUuid, targetName);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            player.sendMessage(PagedMenu.legacy("&cSomething went wrong reading UltimateDonutSmp's data. Check console."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(partial))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
