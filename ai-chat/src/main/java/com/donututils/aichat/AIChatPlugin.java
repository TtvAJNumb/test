package com.donututils.aichat;

import com.donututils.aichat.client.ClaudeApiClient;
import com.donututils.aichat.command.AICommand;
import com.donututils.aichat.config.AIChatConfig;
import com.donututils.aichat.memory.ConversationMemory;
import com.donututils.aichat.tool.ServerContextService;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** A general-purpose in-game AI chat assistant, standalone - no dependency on any other plugin in this
 * repo. Optionally answers server-specific questions using real data from StockMarket/MarketWatch if
 * those happen to be installed, via read-only tool calls (see {@link ServerContextService}). */
public final class AIChatPlugin extends JavaPlugin {

    private ClaudeApiClient apiClient;
    private ConversationMemory memory;
    private volatile ServerContextService serverContext;
    private volatile AIChatConfig config;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        apiClient = new ClaudeApiClient(this);
        memory = new ConversationMemory();
        config = loadConfigValues();
        serverContext = new ServerContextService();

        registerCommand("ai", new AICommand(this, apiClient, memory));

        getLogger().info("AIChat enabled" + (serverContext.hasTools() ? " with server-data tools available." : " (no server-data tools detected - StockMarket/MarketWatch not installed)."));
    }

    public void reloadAIChat() {
        reloadConfig();
        config = loadConfigValues();
        serverContext = new ServerContextService();
    }

    public AIChatConfig getAIChatConfig() {
        return config;
    }

    public ServerContextService getServerContext() {
        return serverContext;
    }

    private AIChatConfig loadConfigValues() {
        FileConfiguration cfg = getConfig();
        return new AIChatConfig(
                cfg.getString("api-key", ""),
                cfg.getString("model", "claude-sonnet-5"),
                cfg.getString("assistant-name", "Astra"),
                cfg.getString("system-prompt", "You are a helpful assistant for this Minecraft server."),
                cfg.getInt("max-tokens", 400),
                cfg.getInt("memory-limit", 6),
                cfg.getInt("cooldown-seconds", 3),
                cfg.getInt("max-message-length", 400)
        );
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            getLogger().warning("plugin.yml is missing the '" + name + "' command definition.");
        }
    }
}
