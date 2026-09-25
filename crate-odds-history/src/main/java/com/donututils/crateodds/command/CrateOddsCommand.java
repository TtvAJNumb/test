package com.donututils.crateodds.command;

import com.donututils.crateodds.gui.OddsMenu;
import com.donututils.crateodds.model.CrateSnapshot;
import com.donututils.crateodds.reflect.UdsBridge;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class CrateOddsCommand implements CommandExecutor, TabCompleter {

    private final UdsBridge bridge;
    private final String titleTemplate;

    public CrateOddsCommand(UdsBridge bridge, String titleTemplate) {
        this.bridge = bridge;
        this.titleTemplate = titleTemplate;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length < 1) {
            List<String> ids = bridge.getCrateIds();
            player.sendMessage(legacy("&cUsage: /crateodds <crate>"));
            if (!ids.isEmpty()) {
                player.sendMessage(legacy("&7Available crates: &f" + String.join("&7, &f", ids)));
            }
            return true;
        }

        String crateId = args[0];
        CrateSnapshot snapshot;
        try {
            snapshot = bridge.getCrateSnapshot(crateId);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            player.sendMessage(legacy("&cCould not read that crate's data. Check console."));
            return true;
        }
        if (snapshot == null) {
            player.sendMessage(legacy("&cNo crate named &f" + crateId + "&c."));
            return true;
        }
        OddsMenu.open(player, titleTemplate, snapshot);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            return bridge.getCrateIds().stream()
                    .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(partial))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
