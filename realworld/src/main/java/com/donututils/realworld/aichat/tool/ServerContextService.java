package com.donututils.realworld.aichat.tool;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Registry of read-only "tools" the AI can call to answer questions with real server data instead of
 * guessing. Every tool here is intentionally curated to expose only public, non-sensitive facts
 * (stock prices, auction house sale stats, a permission-filtered command list) - nothing about
 * player balances, punishment history, or any other plugin's private data. Tools for a given plugin
 * only get registered if that plugin is actually installed and enabled.
 */
public final class ServerContextService {

    private final Map<String, ToolDefinition> tools = new LinkedHashMap<>();

    public ServerContextService() {
        CommandDirectoryContext.registerTools(this);
        // StockMarket is now the "stockmarket" subsystem built into this same RealWorld plugin
        // rather than a separate plugin, so its stocks.yml always exists under RealWorld's own data
        // folder - the tools are unconditionally useful, not gated on a separate plugin being present.
        StockMarketContext.registerTools(this);
        registerIfPresent("MarketWatch", AuctionHouseContext::registerTools);
    }

    private void registerIfPresent(String pluginName, Consumer<ServerContextService> registrar) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin != null && plugin.isEnabled()) {
            registrar.accept(this);
        }
    }

    void register(ToolDefinition tool) {
        tools.put(tool.name(), tool);
    }

    public boolean hasTools() {
        return !tools.isEmpty();
    }

    public List<Object> toolSchemas() {
        List<Object> schemas = new ArrayList<>();
        for (ToolDefinition tool : tools.values()) {
            schemas.add(tool.schema());
        }
        return schemas;
    }

    public String execute(String name, Map<String, String> args, CommandSender sender) {
        ToolDefinition tool = tools.get(name);
        if (tool == null) {
            return "That lookup isn't available on this server.";
        }
        try {
            return tool.executor().execute(args, sender);
        } catch (RuntimeException ex) {
            return "That lookup failed unexpectedly.";
        }
    }
}
