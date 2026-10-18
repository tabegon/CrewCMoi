package fr.crewcmoi.pvp.commands;

import fr.crewcmoi.other.utils.Messages;
import fr.crewcmoi.pvp.managers.DuelArenaManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DuelArenaCommand implements CommandExecutor, TabCompleter {
    private final DuelArenaManager manager;
    public DuelArenaCommand(DuelArenaManager manager) { this.manager = manager; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("crew.admin")) { Messages.send(sender, "moderation.duelarena.no-permission"); return true; }
        int arena = 1; String action;
        if (args.length == 1) action = args[0].toLowerCase();
        else if (args.length == 2 && (args[0].equals("1") || args[0].equals("2"))) { arena = Integer.parseInt(args[0]); action = args[1].toLowerCase(); }
        else { sender.sendMessage("§e/duelarena [1|2] <corner1|corner2|save|reset>"); return true; }
        manager.setActiveArena(arena);
        switch (action) {
            case "corner1", "corner2" -> {
                if (!(sender instanceof Player player)) { Messages.send(sender, "moderation.duelarena.player-only"); return true; }
                int index = action.equals("corner1") ? 1 : 2; manager.setCorner(index, player.getLocation());
                Messages.send(sender, "moderation.duelarena.corner-set", java.util.Map.of("index", index));
                sender.sendMessage("§7Arène sélectionnée : §f" + arena);
                if (manager.isRegionDefined()) Messages.send(sender, "moderation.duelarena.ready-to-save");
            }
            case "save" -> manager.saveSnapshot(sender);
            case "reset" -> {
                if (!manager.hasSnapshot()) { Messages.send(sender, "moderation.duelarena.no-save"); return true; }
                manager.resetArena(); sender.sendMessage("§aRéinitialisation de l'arène " + arena + " lancée.");
                Messages.send(sender, "moderation.duelarena.reset-started");
            }
            default -> sender.sendMessage("§e/duelarena [1|2] <corner1|corner2|save|reset>");
        }
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return Arrays.asList("1", "2", "corner1", "corner2", "save", "reset").stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        if (args.length == 2 && (args[0].equals("1") || args[0].equals("2"))) return Arrays.asList("corner1", "corner2", "save", "reset").stream().filter(s -> s.startsWith(args[1].toLowerCase())).toList();
        return List.of();
    }
}
