package com.donututils.realworld.careers.command;

import com.donututils.realworld.RealWorldPlugin;
import com.donututils.realworld.careers.citizen.CitizenManager;
import com.donututils.realworld.careers.config.CareersConfig;
import com.donututils.realworld.careers.config.JobDefinition;
import com.donututils.realworld.careers.model.CitizenProfile;
import com.donututils.realworld.careers.onboarding.OnboardingGuiService;
import com.donututils.realworld.ledger.economy.LedgerEconomyProvider;
import com.donututils.realworld.ledger.shards.ShardManager;
import com.donututils.realworld.stockmarket.service.PortfolioService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class CareerCommand implements CommandExecutor {

    private final RealWorldPlugin plugin;
    private final CitizenManager citizenManager;
    private final OnboardingGuiService guiService;
    private final LedgerEconomyProvider economy;
    private final PortfolioService stockPortfolioService;
    private final ShardManager shardManager;

    public CareerCommand(RealWorldPlugin plugin, CitizenManager citizenManager, OnboardingGuiService guiService,
                          LedgerEconomyProvider economy, PortfolioService stockPortfolioService, ShardManager shardManager) {
        this.plugin = plugin;
        this.citizenManager = citizenManager;
        this.guiService = guiService;
        this.economy = economy;
        this.stockPortfolioService = stockPortfolioService;
        this.shardManager = shardManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&cUsage: /career [info|list|choose <job>|reopen|legacy|reload]"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "info" -> info(sender);
            case "list" -> list(sender);
            case "choose" -> choose(sender, args);
            case "reopen" -> reopen(sender);
            case "legacy" -> legacy(sender, args);
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
                double multiplier = citizenManager.wageBonusMultiplier(profile, plugin.getCareersConfig());
                sender.sendMessage(color(String.format(Locale.US, "&7Wage: &f$%,.2f &7every %d min",
                        job.wageAmount() * multiplier, job.wageIntervalMinutes())));
            }
        } else {
            sender.sendMessage(color("&7Job: &cnone - use /career reopen to pick one"));
        }
        if (profile.legacyCount() > 0) {
            sender.sendMessage(color("&7Legacies: &d" + profile.legacyCount()
                    + " &7(a permanent +" + String.format(Locale.US, "%.0f", plugin.getCareersConfig().legacyWageBonusPercentPerLegacy() * profile.legacyCount())
                    + "% wage bonus)"));
        }
        if (profile.ageTier() == com.donututils.realworld.careers.config.AgeTier.MINOR) {
            int threshold = plugin.getCareersConfig().minorToAdultPlaytimeMinutes();
            sender.sendMessage(color("&7Playtime: &f" + profile.playtimeMinutes() + "&7/&f" + threshold
                    + " &7min until Adulthood"));
        }
    }

    private void legacy(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        CitizenProfile profile = citizenManager.profileOf(player.getUniqueId());
        if (profile.ageTier() != com.donututils.realworld.careers.config.AgeTier.ADULT) {
            sender.sendMessage(color("&cOnly Adults can reset their life as a legacy."));
            return;
        }
        double money = economy.getBalance(player);
        double stockValue = stockPortfolioService.getPortfolio(player.getUniqueId()).holdingsValue();
        double totalWealth = money + stockValue;
        double percent = plugin.getCareersConfig().legacyInheritancePercent();
        double projectedInheritance = totalWealth * (percent / 100.0);

        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            sender.sendMessage(color("&e&lLegacy Reset"));
            sender.sendMessage(color("&7This resets your age tier back to Minor and clears your job."));
            sender.sendMessage(color("&7Your Money ($" + String.format(Locale.US, "%,.2f", money)
                    + ") and stock portfolio value ($" + String.format(Locale.US, "%,.2f", stockValue)
                    + ") are wiped, but you keep " + String.format(Locale.US, "%.0f", percent)
                    + "% of that total as an inheritance: &a$" + String.format(Locale.US, "%,.2f", projectedInheritance)));
            sender.sendMessage(color("&7Your stock shares themselves are NOT sold or touched."));
            sender.sendMessage(color("&7You also permanently gain a small wage bonus on every future life, and some Shards."));
            sender.sendMessage(color("&cThis cannot be undone. &7Type &f/career legacy confirm &7to proceed."));
            return;
        }

        CitizenManager.LegacyResult result = citizenManager.performLegacyReset(player.getUniqueId(), totalWealth);
        if (result == null) {
            sender.sendMessage(color("&cOnly Adults can reset their life as a legacy."));
            return;
        }
        economy.withdrawPlayer(player, money);
        economy.depositPlayer(player, result.inheritanceMoney());
        shardManager.credit(player.getUniqueId(), result.shardBonus());

        player.sendMessage(color("&6&lYour life resets as a legacy. &7Inheritance: &a$"
                + String.format(Locale.US, "%,.2f", result.inheritanceMoney()) + " &7+ &d" + result.shardBonus()
                + " Shards&7. Legacy count: &d" + result.newLegacyCount()));
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
