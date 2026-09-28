package com.donututils.realworld.aichat.command;

import com.donututils.realworld.RealWorldPlugin;
import com.donututils.realworld.aichat.client.ChatClient;
import com.donututils.realworld.aichat.config.AIChatConfig;
import com.donututils.realworld.aichat.memory.ConversationMemory;
import com.donututils.realworld.aichat.tool.ServerContextService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AICommand implements CommandExecutor {

    private final RealWorldPlugin plugin;
    private final ConversationMemory memory;
    private final Map<UUID, Long> lastRequestMillis = new ConcurrentHashMap<>();

    public AICommand(RealWorldPlugin plugin, ConversationMemory memory) {
        this.plugin = plugin;
        this.memory = memory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&cUsage: /ai <message> | /ai reset" + (sender.hasPermission("aichat.admin") ? " | /ai reload" : "")));
            return true;
        }

        if (args[0].equalsIgnoreCase("reset")) {
            if (sender instanceof Player player) {
                memory.reset(player.getUniqueId());
                sender.sendMessage(color("&aYour conversation with " + plugin.getAIChatConfig().assistantName() + " has been reset."));
            } else {
                sender.sendMessage(color("&cOnly players have a conversation to reset."));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("aichat.admin")) {
                sender.sendMessage(color("&cYou do not have permission to reload AIChat."));
                return true;
            }
            plugin.reloadAIChat();
            sender.sendMessage(color("&aAIChat config reloaded."));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can chat with the AI assistant from in-game."));
            return true;
        }

        AIChatConfig config = plugin.getAIChatConfig();
        boolean needsApiKey = !"ollama".equalsIgnoreCase(config.provider());
        if (needsApiKey && (config.apiKey() == null || config.apiKey().isBlank())) {
            sender.sendMessage(color("&cThe AI assistant isn't configured yet - ask an admin to set api-key in AIChat's config.yml."));
            return true;
        }

        String message = String.join(" ", args);
        if (message.length() > config.maxMessageLength()) {
            sender.sendMessage(color("&cThat message is too long (max " + config.maxMessageLength() + " characters)."));
            return true;
        }

        long now = System.currentTimeMillis();
        Long last = lastRequestMillis.get(player.getUniqueId());
        long cooldownMillis = config.cooldownSeconds() * 1000L;
        if (last != null && now - last < cooldownMillis) {
            long remaining = (cooldownMillis - (now - last) + 999) / 1000;
            sender.sendMessage(color("&cSlow down - wait " + remaining + "s before asking " + config.assistantName() + " again."));
            return true;
        }
        lastRequestMillis.put(player.getUniqueId(), now);

        UUID playerId = player.getUniqueId();
        List<ConversationMemory.Message> history = memory.get(playerId);
        ServerContextService context = plugin.getServerContext();
        ChatClient chatClient = plugin.getChatClient();

        chatClient.ask(config.apiKey(), config.model(), config.maxTokens(), config.systemPrompt(), history, message, context, sender)
                .thenAccept(reply -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Player target = plugin.getServer().getPlayer(playerId);
                    if (target == null) {
                        return;
                    }
                    if (reply.success()) {
                        memory.record(playerId, message, reply.text(), config.memoryLimit());
                        target.sendMessage(color("&b" + config.assistantName() + "&7: &f" + reply.text()));
                    } else {
                        target.sendMessage(color("&c" + reply.text()));
                    }
                }));

        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
