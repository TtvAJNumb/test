package com.donututils.donutrep.crates;

import com.donututils.donutrep.DonutREPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/** /crate - real UDS's crate-definition/management command (create/delete/type/open/keys/reload/key/
 * take/set/keyall), not to be confused with /cratebind, DonutREP's own CrateBindAddon-derived command
 * for binding an existing chest/barrel/ender chest/shulker box to a crate - the two features used to
 * share this command name before being split apart to match real UDS exactly. */
public final class CrateCommand implements CommandExecutor {

    private final DonutREPPlugin plugin;
    private final CrateManager crateManager;

    public CrateCommand(DonutREPPlugin plugin, CrateManager crateManager) {
        this.plugin = plugin;
        this.crateManager = crateManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&cUsage: /crate <create|delete|type|open|keys|key|take|set|keyall|reload>"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> create(sender, args);
            case "delete" -> delete(sender, args);
            case "type" -> type(sender, args);
            case "open" -> open(sender, args);
            case "keys" -> keys(sender, args);
            case "key" -> key(sender, args);
            case "take" -> take(sender, args);
            case "set" -> set(sender, args);
            case "keyall" -> keyall(sender, args);
            case "reload" -> reload(sender);
            default -> sender.sendMessage(color("&cUnknown /crate subcommand."));
        }
        return true;
    }

    private void create(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /crate create <id>"));
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (plugin.getConfig().contains("crates." + id)) {
            sender.sendMessage(color("&cA crate named &f" + id + " &calready exists."));
            return;
        }
        plugin.getConfig().set("crates." + id + ".display-name", args[1]);
        plugin.getConfig().set("crates." + id + ".key-material", "TRIPWIRE_HOOK");
        plugin.getConfig().set("crates." + id + ".type", "DEFAULT");
        plugin.saveConfig();
        plugin.reloadCrates();
        sender.sendMessage(color("&aCreated crate &f" + id + "&a. Add rewards under crates." + id
                + ".rewards in config.yml, then /crate reload."));
    }

    private void delete(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /crate delete <id>"));
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (!plugin.getConfig().contains("crates." + id)) {
            sender.sendMessage(color("&cNo crate named &f" + id + "&c."));
            return;
        }
        plugin.getConfig().set("crates." + id, null);
        plugin.saveConfig();
        plugin.reloadCrates();
        sender.sendMessage(color("&aDeleted crate &f" + id + "&a."));
    }

    private void type(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(color("&cUsage: /crate type <id> <type>"));
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (plugin.getCrateConfig().crate(id) == null) {
            sender.sendMessage(color("&cNo crate named &f" + id + "&c."));
            return;
        }
        plugin.getConfig().set("crates." + id + ".type", args[2].toUpperCase(Locale.ROOT));
        plugin.saveConfig();
        plugin.reloadCrates();
        sender.sendMessage(color("&aSet &f" + id + "&a's type to &f" + args[2].toUpperCase(Locale.ROOT)
                + "&a. (Opening animation still uses the default roll - only the label changes for now.)"));
    }

    private void open(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /crate open <id> [player]"));
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (plugin.getCrateConfig().crate(id) == null) {
            sender.sendMessage(color("&cNo crate named &f" + id + "&c."));
            return;
        }
        Player target;
        if (args.length >= 3) {
            if (!requireAdmin(sender)) {
                return;
            }
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(color("&c" + args[2] + " isn't online."));
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(color("&cUsage: /crate open <id> <player>"));
            return;
        }
        if (!crateManager.tryOpen(target, id)) {
            sender.sendMessage(color("&c" + target.getName() + " doesn't have a " + id + " crate key."));
        }
    }

    private void keys(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(color("&c" + args[1] + " isn't online."));
                return;
            }
            if (target != sender && !sender.hasPermission("ultimatedonutsmp.admin.crate")) {
                sender.sendMessage(color("&cYou do not have permission to check another player's keys."));
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(color("&cUsage: /crate keys <player>"));
            return;
        }

        String filterId = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : null;
        sender.sendMessage(color("&6&l" + target.getName() + "'s Crate Keys"));
        boolean any = false;
        for (CrateDefinition crate : plugin.getCrateConfig().crates().values()) {
            if (filterId != null && !crate.id().equalsIgnoreCase(filterId)) {
                continue;
            }
            int count = crateManager.countKeys(target, crate.id());
            if (count > 0 || filterId != null) {
                sender.sendMessage(color("&e" + crate.id() + "&7: &f" + count));
                any = true;
            }
        }
        if (!any) {
            sender.sendMessage(color("&7No keys held."));
        }
    }

    private void key(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /crate key <give|take> <player> <id> <amount>"));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "give" -> giveKeys(sender, shift(args));
            case "take" -> takeKeys(sender, shift(args));
            default -> sender.sendMessage(color("&cUsage: /crate key <give|take> <player> <id> <amount>"));
        }
    }

    private void take(CommandSender sender, String[] args) {
        takeKeys(sender, args);
    }

    @SuppressWarnings("deprecation")
    private void giveKeys(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(color("&cUsage: /crate set <player> <crate> <amount>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        Player online = target.getName() != null ? Bukkit.getPlayer(target.getUniqueId()) : null;
        if (online == null) {
            sender.sendMessage(color("&c" + args[1] + " must be online to receive keys."));
            return;
        }
        String id = args[2].toLowerCase(Locale.ROOT);
        if (plugin.getCrateConfig().crate(id) == null) {
            sender.sendMessage(color("&cNo crate named &f" + id + "&c."));
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(color("&cInvalid amount."));
            return;
        }
        crateManager.giveKeys(online, id, amount);
        sender.sendMessage(color("&aGave " + amount + "x " + id + " key(s) to " + args[1] + "."));
    }

    @SuppressWarnings("deprecation")
    private void takeKeys(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(color("&cUsage: /crate take <player> <crate> <amount>"));
            return;
        }
        Player online = Bukkit.getPlayer(args[1]);
        if (online == null) {
            sender.sendMessage(color("&c" + args[1] + " isn't online."));
            return;
        }
        String id = args[2].toLowerCase(Locale.ROOT);
        if (plugin.getCrateConfig().crate(id) == null) {
            sender.sendMessage(color("&cNo crate named &f" + id + "&c."));
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(color("&cInvalid amount."));
            return;
        }
        int removed = crateManager.removeKeys(online, id, amount);
        sender.sendMessage(color("&aRemoved " + removed + "x " + id + " key(s) from " + args[1] + "."));
    }

    @SuppressWarnings("deprecation")
    private void set(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(color("&cUsage: /crate set <player> <crate> <amount>"));
            return;
        }
        Player online = Bukkit.getPlayer(args[1]);
        if (online == null) {
            sender.sendMessage(color("&c" + args[1] + " isn't online."));
            return;
        }
        String id = args[2].toLowerCase(Locale.ROOT);
        if (plugin.getCrateConfig().crate(id) == null) {
            sender.sendMessage(color("&cNo crate named &f" + id + "&c."));
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(color("&cInvalid amount."));
            return;
        }
        int current = crateManager.countKeys(online, id);
        if (amount > current) {
            crateManager.giveKeys(online, id, amount - current);
        } else if (amount < current) {
            crateManager.removeKeys(online, id, current - amount);
        }
        sender.sendMessage(color("&aSet " + args[1] + "'s " + id + " keys to " + amount + "."));
    }

    private void keyall(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(color("&cUsage: /crate keyall <id> <amount>"));
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (plugin.getCrateConfig().crate(id) == null) {
            sender.sendMessage(color("&cNo crate named &f" + id + "&c."));
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(color("&cInvalid amount."));
            return;
        }
        int count = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            crateManager.giveKeys(online, id, amount);
            count++;
        }
        sender.sendMessage(color("&aGave " + amount + "x " + id + " key(s) to " + count + " online player(s)."));
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("ultimatedonutsmp.admin.crate.reload")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return;
        }
        plugin.reloadCrates();
        sender.sendMessage(color("&aCrates config reloaded."));
    }

    private boolean requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("ultimatedonutsmp.admin.crate")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return false;
        }
        return true;
    }

    /** Drops args[0] (the "give"/"take" selector already consumed by {@link #key}) so the shared
     * give/take helpers see the same <player> <crate> <amount> shape as /crate take. */
    private static String[] shift(String[] args) {
        String[] copy = new String[args.length - 1];
        copy[0] = args[0];
        System.arraycopy(args, 2, copy, 1, args.length - 2);
        return copy;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
