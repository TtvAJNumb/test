package com.donututils.donutrep.util;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Translates both legacy '&' color codes and modern '&#RRGGBB' hex codes - real UDS configs (shop.yml's
 * category DISPLAY-NAME entries, for example) use hex codes, which {@link ChatColor#translateAlternateColorCodes}
 * alone doesn't understand. Hex codes are expanded to the raw section-sign sequence Minecraft's client
 * renders as a 24-bit color (&#RRGGBB -> §x§R§R§G§G§B§B), then the rest is run through the normal
 * legacy-code translator. */
public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private ColorUtil() {
    }

    public static String color(String text) {
        if (text == null) {
            return "";
        }
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}
