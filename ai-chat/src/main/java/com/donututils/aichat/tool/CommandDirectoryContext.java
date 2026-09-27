package com.donututils.aichat.tool;

import com.donututils.aichat.json.MiniJson;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.Locale;
import java.util.Map;

/**
 * Lists commands the asking player already has permission to use, straight from each loaded plugin's
 * registered command list (plugin.yml). Nothing sensitive here - a command the sender can't use is
 * simply left out, using the same permission check the server itself would apply.
 */
final class CommandDirectoryContext {

    private CommandDirectoryContext() {
    }

    @SuppressWarnings("unchecked")
    static void registerTools(ServerContextService service) {
        service.register(new ToolDefinition(
                "list_server_commands",
                (Map<String, Object>) MiniJson.parse("""
                        {"name":"list_server_commands","description":"List commands on this server that the asking player has permission to use, optionally filtered by a keyword in the command name or description.","input_schema":{"type":"object","properties":{"search":{"type":"string","description":"Optional keyword to filter command names/descriptions by"}}}}
                        """),
                (args, sender) -> listCommands(args.get("search"), sender)
        ));
    }

    private static String listCommands(String search, CommandSender sender) {
        String keyword = search == null ? null : search.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (!plugin.isEnabled()) {
                continue;
            }
            PluginDescriptionFile description = plugin.getDescription();
            Map<String, Map<String, Object>> commands = description == null ? null : description.getCommands();
            if (commands == null) {
                continue;
            }
            for (Map.Entry<String, Map<String, Object>> entry : commands.entrySet()) {
                String name = entry.getKey();
                Map<String, Object> meta = entry.getValue();
                Object permission = meta == null ? null : meta.get("permission");
                if (permission != null && sender != null && !sender.hasPermission(String.valueOf(permission))) {
                    continue;
                }
                Object descriptionValue = meta == null ? null : meta.get("description");
                String desc = descriptionValue == null ? "" : String.valueOf(descriptionValue);
                if (keyword != null && !name.toLowerCase(Locale.ROOT).contains(keyword) && !desc.toLowerCase(Locale.ROOT).contains(keyword)) {
                    continue;
                }
                if (count > 0) {
                    out.append("; ");
                }
                out.append('/').append(name);
                if (!desc.isBlank()) {
                    out.append(" - ").append(desc);
                }
                count++;
                if (count >= 40) {
                    break;
                }
            }
            if (count >= 40) {
                break;
            }
        }
        if (count == 0) {
            return keyword == null ? "No commands found." : "No commands found matching \"" + search + "\".";
        }
        return out.toString();
    }
}
