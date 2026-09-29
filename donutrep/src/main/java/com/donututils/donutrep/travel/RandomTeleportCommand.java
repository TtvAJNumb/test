package com.donututils.donutrep.travel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /randomteleport <player> - staff command, forces the given player through a random teleport
 * (the same search RtpManager uses for /rtp, just triggered on someone else's behalf). */
public final class RandomTeleportCommand implements CommandExecutor {

    private final RtpManager rtpManager;

    public RandomTeleportCommand(RtpManager rtpManager) {
        this.rtpManager = rtpManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("travel.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(color("&cUsage: /randomteleport <player>"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(color("&c" + args[0] + " isn't online."));
            return true;
        }
        RtpManager.Result result = rtpManager.attempt(target, () -> target.sendMessage(color("&aTeleported to a random location.")));
        switch (result) {
            case TELEPORTED -> sender.sendMessage(color("&aRandom-teleporting " + target.getName() + "..."));
            case ON_COOLDOWN -> sender.sendMessage(color("&c" + target.getName() + " is still on RTP cooldown."));
            case BUSY -> sender.sendMessage(color("&cToo many random-teleports in flight - try again shortly."));
            case FAILED -> sender.sendMessage(color("&cCouldn't find a safe location."));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
