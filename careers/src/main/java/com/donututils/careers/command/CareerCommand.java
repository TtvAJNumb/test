package com.donututils.careers.command;

import com.donututils.careers.CareersPlugin;
import com.donututils.careers.citizen.CitizenManager;
import com.donututils.careers.config.CareersConfig;
import com.donututils.careers.config.JobDefinition;
import com.donututils.careers.model.CitizenProfile;
import com.donututils.careers.onboarding.OnboardingGuiService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class CareerCommand implements CommandExecutor {

    private final CareersPlugin plugin;
    private final CitizenManager citizenManager;
    private final OnboardingGuiService guiService;

    public CareerCommand(CareersPlugin plugin, CitizenManager citizenManager, OnboardingGuiService guiService) {
        this.plugin = plugin;
        this.citizenManager = citizenManager;
        this.guiService = guiService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&cUsage: /career [info|list|choose <job>|reopen|reload]"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "info" -> info(sender);
            case "list" -> list(sender);
            case "choose" -> choose(sender, args);
            case "reopen" -> reopen(sender);
            case "reload" -> reload(sender);
            default -> sender.sendMessage(color("&cUnknown /career subcommand."));
        }
        return true;
    }

    private void info(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        CitizenProfile profile = citizenManager.profileOf(player.getUniqueId());
        if (!profile.hasChosenAgeTier()) {
            sender.sendMessage(color("&cYou haven't picked an age tier yet - use /career reopen."));
            return;
        }
        sender.sendMessage(color("&6&lYour Career"));
        sender.sendMessage(color("&7Age tier: &f" + profile.ageTier()));
        if (profile.hasChosenJob()) {
            JobDefinition job = plugin.getCareersConfig().job(profile.jobId());
            String name = job != null ? job.displayName() : profile.jobId();
            sender.sendMessage(color("&7Job: " + name));
            if (job != null) {
                sender.sendMessage(color(String.format(Locale.US, "&7Wage: &f$%,.2f &7every %d min",
                        job.wageAmount(), job.wageIntervalMinutes())));
            }
        } else {
            sender.sendMessage(color("&7Job: &cnone - use /career reopen to pick one"));
        }
    }

    private void list(CommandSender sender) {
        CareersConfig config = plugin.getCareersConfig();
        sender.sendMessage(color("&6&lJobs"));
        for (JobDefinition job : config.jobs().values()) {
            sender.sendMessage(color("&e" + job.id() + " &7(" + job.minAgeTier() + "+) - " + job.displayName()
                    + String.format(Locale.US, " &7$%,.2f/%d min", job.wageAmount(), job.wageIntervalMinutes())));
        }
    }

    private void choose(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /career choose <job>"));
            return;
        }
        CitizenProfile profile = citizenManager.profileOf(player.getUniqueId());
        if (!profile.hasChosenAgeTier()) {
            sender.sendMessage(color("&cPick an age tier first - use /career reopen."));
            return;
        }
        JobDefinition job = plugin.getCareersConfig().job(args[1]);
        if (job == null) {
            sender.sendMessage(color("&cUnknown job: " + args[1]));
            return;
        }
        if (!job.availableTo(profile.ageTier())) {
            sender.sendMessage(color("&cThat job requires being an Adult."));
            return;
        }
        citizenManager.setJob(player.getUniqueId(), job.id());
        sender.sendMessage(color("&aYou're now working as a " + job.displayName() + "&a."));
    }

    private void reopen(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        CitizenProfile profile = citizenManager.profileOf(player.getUniqueId());
        if (!profile.hasChosenAgeTier()) {
            guiService.openAgeMenu(player);
        } else {
            guiService.openJobMenu(player);
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("careers.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return;
        }
        plugin.reloadCareers();
        sender.sendMessage(color("&aCareers config reloaded."));
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(color("&cOnly players can do that."));
        return null;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
