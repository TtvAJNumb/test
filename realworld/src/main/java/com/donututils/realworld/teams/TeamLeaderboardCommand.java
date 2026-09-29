package com.donututils.realworld.teams;

import com.donututils.realworld.ledger.economy.LedgerEconomyProvider;
import com.donututils.realworld.ledger.shards.ShardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** /teambaltop and /teamshardstop: ranks every team by its members' combined Money or Shards. A
 * simple chat-printed top-10 rather than a full paginated GUI - the original TeamLeaderboard's menu
 * was built around UltimateDonutSmp's own leaderboard cache; this is a smaller, fresh implementation
 * against RealWorld's own Ledger/Shards data. */
public final class TeamLeaderboardCommand implements CommandExecutor {

    public enum Stat { MONEY, SHARDS }

    private final TeamManager teamManager;
    private final LedgerEconomyProvider economy;
    private final ShardManager shardManager;
    private final Stat stat;

    public TeamLeaderboardCommand(TeamManager teamManager, LedgerEconomyProvider economy, ShardManager shardManager, Stat stat) {
        this.teamManager = teamManager;
        this.economy = economy;
        this.shardManager = shardManager;
        this.stat = stat;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<Team> teams = teamManager.allTeams();
        if (teams.isEmpty()) {
            sender.sendMessage(color("&7No teams exist yet."));
            return true;
        }

        Map<UUID, Double> moneyByPlayer = economy.allCheckingBalances();
        Map<UUID, Long> shardsByPlayer = shardManager.allBalances();

        record Standing(String name, double value) {
        }
        List<Standing> standings = teams.stream()
                .map(team -> new Standing(team.name(), team.memberIds().stream()
                        .mapToDouble(id -> stat == Stat.MONEY ? moneyByPlayer.getOrDefault(id, 0.0) : shardsByPlayer.getOrDefault(id, 0L))
                        .sum()))
                .sorted(Comparator.comparingDouble(Standing::value).reversed())
                .limit(10)
                .toList();

        sender.sendMessage(color(stat == Stat.MONEY ? "&6&lTeam Money Leaderboard" : "&d&lTeam Shards Leaderboard"));
        int rank = 1;
        for (Standing standing : standings) {
            String value = stat == Stat.MONEY
                    ? "$" + String.format(Locale.US, "%,.2f", standing.value())
                    : String.format(Locale.US, "%,.0f", standing.value()) + " Shards";
            sender.sendMessage(color("&e#" + rank + " &f" + standing.name() + " &7- " + value));
            rank++;
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
