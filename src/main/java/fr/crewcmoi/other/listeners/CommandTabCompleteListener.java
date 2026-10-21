package fr.crewcmoi.other.listeners;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CommandTabCompleteListener implements Listener {
    private static final Map<String, List<String>> ROOT_ARGUMENTS = Map.ofEntries(
            Map.entry("ah", List.of("help", "sell")),
            Map.entry("team", List.of("create", "add", "accept", "deny", "leave", "disband", "info")),
            Map.entry("bounty", List.of("add", "review")),
            Map.entry("claim", List.of("trust", "untrust", "info", "settings", "see", "shop", "sell", "buy", "ah")),
            Map.entry("claims", List.of("settings")),
            Map.entry("money", List.of("add", "remove", "set")),
            Map.entry("rank", List.of("list", "set", "clear")),
            Map.entry("staff", List.of("info")),
            Map.entry("reset", List.of("quest")),
            Map.entry("duelarena", List.of("1", "2", "corner1", "corner2", "save", "reset")),
            Map.entry("spectate", List.of("stop"))
    );

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        CommandSender sender = event.getSender();
        String buffer = event.getBuffer();
        if (buffer == null || !buffer.startsWith("/")) return;
        String[] parts = buffer.substring(1).split(" ", -1);
        String command = parts[0].toLowerCase(Locale.ROOT);
        String partial = parts.length == 1 ? "" : parts[parts.length - 1];
        int argumentIndex = parts.length - 1;

        List<String> candidates = new ArrayList<>();
        if (argumentIndex == 1) {
            candidates.addAll(ROOT_ARGUMENTS.getOrDefault(command, List.of()));
            if (List.of("balance", "bal", "pay", "tpa", "tpahere", "money", "rank", "info",
                    "information", "infos", "homeadmin", "playeradmin", "reset", "duel", "spectate").contains(command)) {
                for (Player player : Bukkit.getOnlinePlayers()) candidates.add(player.getName());
            }
        } else if (argumentIndex == 2) {
            if (List.of("pay", "money", "rank", "team", "bounty", "claim", "claims", "reset", "info", "tpa", "tpahere", "duel").contains(command)) {
                for (Player player : Bukkit.getOnlinePlayers()) candidates.add(player.getName());
            }
            if (command.equals("money") && parts.length > 1 && parts[1].equalsIgnoreCase("add")
                    || command.equals("money") && parts.length > 1 && parts[1].equalsIgnoreCase("remove")
                    || command.equals("money") && parts.length > 1 && parts[1].equalsIgnoreCase("set")) {
                candidates.add("<montant>");
            }
            if (command.equals("rank") && parts.length > 1 && parts[1].equalsIgnoreCase("set")) {
                candidates.addAll(Arrays.asList("player", "vip", "mod", "dev", "fonda", "tester", "youtubeur"));
            }
        }
        if (candidates.isEmpty()) return;
        String lower = partial.toLowerCase(Locale.ROOT);
        List<String> filtered = candidates.stream().distinct()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
        if (!filtered.isEmpty()) event.setCompletions(filtered);
    }
}
