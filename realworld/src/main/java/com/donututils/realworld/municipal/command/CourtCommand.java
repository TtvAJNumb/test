package com.donututils.realworld.municipal.command;

import com.donututils.realworld.municipal.config.MunicipalConfig;
import com.donututils.realworld.municipal.court.CourtManager;
import com.donututils.realworld.municipal.jail.JailManager;
import com.donututils.realworld.municipal.model.CourtCase;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Supplier;

public final class CourtCommand implements CommandExecutor {

    private final Plugin plugin;
    private final CourtManager courtManager;
    private final JailManager jailManager;
    private final Supplier<MunicipalConfig> configSupplier;

    public CourtCommand(Plugin plugin, CourtManager courtManager, JailManager jailManager, Supplier<MunicipalConfig> configSupplier) {
        this.plugin = plugin;
        this.courtManager = courtManager;
        this.jailManager = jailManager;
        this.configSupplier = configSupplier;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&cUsage: /court [sentence <caseId> <minutes> <reason>|dismiss <caseId> [reason]|release <player>|record <player>]"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "sentence" -> sentence(sender, args);
            case "dismiss" -> dismiss(sender, args);
            case "release" -> release(sender, args);
            case "record" -> record(sender, args);
            default -> sender.sendMessage(color("&cUnknown /court subcommand."));
        }
        return true;
    }

    private void sentence(CommandSender sender, String[] args) {
        if (!(sender instanceof Player judge)) {
            sender.sendMessage(color("&cOnly players can preside over trials."));
            return;
        }
        if (args.length < 4) {
            judge.sendMessage(color("&cUsage: /court sentence <caseId> <minutes> <reason>"));
            return;
        }
        long caseId;
        int minutes;
        try {
            caseId = Long.parseLong(args[1]);
            minutes = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            judge.sendMessage(color("&cInvalid case id or minutes."));
            return;
        }
        String reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        int maxSentence = configSupplier.get().maxSentenceMinutes();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            CourtManager.VerdictResult result = courtManager.sentence(judge.getUniqueId(), caseId, minutes, reason, maxSentence);
            Bukkit.getScheduler().runTask(plugin, () -> {
                judge.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
                if (!result.success()) {
                    return;
                }
                CourtCase courtCase = result.courtCase();
                Player defendant = Bukkit.getServer().getPlayer(courtCase.defendantId());
                if (defendant == null) {
                    judge.sendMessage(color("&eThe defendant is offline - they'll need to be jailed manually when they're next online (jailing requires them online to record where to return them afterward)."));
                    return;
                }
                jailManager.jailPlayer(defendant, courtCase.id(), courtCase.sentenceMinutes());
                defendant.sendMessage(color("&cConvicted: " + courtCase.verdictReason() + " &7- jailed for " + courtCase.sentenceMinutes() + " minute(s)."));
            });
        });
    }

    private void dismiss(CommandSender sender, String[] args) {
        if (!(sender instanceof Player judge)) {
            sender.sendMessage(color("&cOnly players can dismiss cases."));
            return;
        }
        if (args.length < 2) {
            judge.sendMessage(color("&cUsage: /court dismiss <caseId> [reason]"));
            return;
        }
        long caseId;
        try {
            caseId = Long.parseLong(args[1]);
        } catch (NumberFormatException ex) {
            judge.sendMessage(color("&cInvalid case id."));
            return;
        }
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Dismissed by the court";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            CourtManager.VerdictResult result = courtManager.dismiss(judge.getUniqueId(), caseId, reason);
            Bukkit.getScheduler().runTask(plugin, () -> judge.sendMessage(color((result.success() ? "&a" : "&c") + result.message())));
        });
    }

    @SuppressWarnings("deprecation")
    private void release(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /court release <player>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!jailManager.isJailed(target.getUniqueId())) {
            sender.sendMessage(color("&c" + args[1] + " isn't currently jailed."));
            return;
        }
        jailManager.releasePlayer(target.getUniqueId());
        sender.sendMessage(color("&aReleased " + args[1] + " early."));
    }

    @SuppressWarnings("deprecation")
    private void record(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /court record <player>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        var cases = courtManager.recordFor(target.getUniqueId());
        sender.sendMessage(color("&6&l" + args[1] + "'s Criminal Record"));
        if (cases.isEmpty()) {
            sender.sendMessage(color("&7Clean record."));
            return;
        }
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

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
