package com.donututils.teamleaderboard.command;

import com.donututils.teamleaderboard.config.TeamLeaderboardConfig;
import com.donututils.teamleaderboard.gui.PagedMenu;
import com.donututils.teamleaderboard.gui.TeamLeaderboardMenuBuilder;
import com.donututils.teamleaderboard.gui.TeamMenuHolder;
import com.donututils.teamleaderboard.model.StatKind;
import com.donututils.teamleaderboard.model.TeamStanding;
import com.donututils.teamleaderboard.service.TeamLeaderboardService;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/** Shared executor for /teambaltop and /teamshardstop - only the {@link StatKind} differs. */
public final class TeamLeaderboardCommand implements CommandExecutor {

    private final TeamLeaderboardService service;
    private final StatKind kind;
    private final Supplier<TeamLeaderboardConfig> configSupplier;

    public TeamLeaderboardCommand(TeamLeaderboardService service, StatKind kind, Supplier<TeamLeaderboardConfig> configSupplier) {
        this.service = service;
        this.kind = kind;
        this.configSupplier = configSupplier;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        List<TeamStanding> standings = service.getStandings(kind);
        List<ItemStack> items;
        if (standings.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            var meta = empty.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(PagedMenu.legacy(configSupplier.get().noTeamsMessage()));
                empty.setItemMeta(meta);
            }
            items = List.of(empty);
        } else {
            items = TeamLeaderboardMenuBuilder.build(standings, kind);
        }

        PagedMenu.open(player, new TeamMenuHolder(), kind.getMenuTitle(), items);
        return true;
    }
}
