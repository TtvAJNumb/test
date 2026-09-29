package com.donututils.donutrep.travel;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /rtp - teleports you to a random nearby location. If too many random-teleports are already in
 * flight, tells the player to run /rtpq instead. */
public final class RtpCommand implements CommandExecutor {

    private final RtpManager rtpManager;

    public RtpCommand(RtpManager rtpManager) {
        this.rtpManager = rtpManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        RtpManager.Result result = rtpManager.attempt(player, () -> player.sendMessage(color("&aTeleported to a random location.")));
        switch (result) {
            case TELEPORTED -> player.sendMessage(color("&7Searching for a safe spot..."));
            case ON_COOLDOWN -> player.sendMessage(color("&cYou need to wait before using /rtp again."));
            case BUSY -> player.sendMessage(color("&cToo many players are random-teleporting right now - try /rtpq to queue instead."));
            case FAILED -> player.sendMessage(color("&cCouldn't find a safe location, try again."));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
