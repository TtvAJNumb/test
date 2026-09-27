package com.donututils.aichat.client;

import com.donututils.aichat.memory.ConversationMemory;
import com.donututils.aichat.tool.ServerContextService;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** A backing AI provider AIChat can talk to. Implementations own their own request/response format. */
public interface ChatClient {

    CompletableFuture<Reply> ask(String apiKey, String model, int maxTokens, String systemPrompt,
                                  List<ConversationMemory.Message> history, String userMessage,
                                  ServerContextService context, CommandSender sender);
}
