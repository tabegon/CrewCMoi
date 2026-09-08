package fr.crewcmoi.other.utils;

import fr.crewcmoi.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Map;

public final class Messages {
    private Messages() {}
    public static String get(String path) {
        return Main.getInstance().getMessages().getString(path, "");
    }
    public static String get(String path, Map<String, ?> placeholders) {
        String value = get(path);
        for (Map.Entry<String, ?> entry : placeholders.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return value;
    }
    public static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }

    public static void send(CommandSender sender, String path) {
        send(sender, path, Map.of(), false);
    }
    public static void send(CommandSender sender, String path, Map<String, ?> placeholders) {
        send(sender, path, placeholders, false);
    }
    public static void send(CommandSender sender, String path, boolean withPrefix) {
        send(sender, path, Map.of(), withPrefix);
    }
    public static void send(CommandSender sender, String path, Map<String, ?> placeholders, boolean withPrefix) {
        String message = get(path, placeholders);
        if (withPrefix) message = get("prefix") + message;
        sender.sendMessage(color(message));
    }
}
