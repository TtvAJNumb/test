package com.donututils.realworld.aichat.tool;

import org.bukkit.command.CommandSender;

import java.util.Map;

@FunctionalInterface
public interface ToolExecutor {
    /** Returns a short, human-readable answer for the model to read back to the player. */
    String execute(Map<String, String> args, CommandSender sender);
}
