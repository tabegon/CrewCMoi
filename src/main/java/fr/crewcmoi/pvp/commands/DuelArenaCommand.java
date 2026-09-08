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

    private final DuelArenaManager duelArenaManager;

    public DuelArenaCommand(DuelArenaManager duelArenaManager) {
        this.duelArenaManager = duelArenaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("crew.admin")) {
            Messages.send(sender, "moderation.duelarena.no-permission");
            return true;
        }

        if (args.length != 1) {
            Messages.send(sender, "moderation.duelarena.usage");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "corner1", "corner2" -> {
                if (!(sender instanceof Player player)) {
                    Messages.send(sender, "moderation.duelarena.player-only");
                    return true;
                }
                int index = sub.equals("corner1") ? 1 : 2;
                duelArenaManager.setCorner(index, player.getLocation());
                Messages.send(sender, "moderation.duelarena.corner-set", java.util.Map.of("index", index));
                if (duelArenaManager.isRegionDefined()) {
                    Messages.send(sender, "moderation.duelarena.ready-to-save");
                }
            }
            case "save" -> duelArenaManager.saveSnapshot(sender);
            case "reset" -> {
                if (!duelArenaManager.hasSnapshot()) {
                    Messages.send(sender, "moderation.duelarena.no-save");
                    return true;
                }
                duelArenaManager.resetArena();
                Messages.send(sender, "moderation.duelarena.reset-started");
            }
            default -> Messages.send(sender, "moderation.duelarena.usage");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = Arrays.asList("corner1", "corner2", "save", "reset");
            List<String> matches = new ArrayList<>();
            for (String option : options) {
                if (option.startsWith(args[0].toLowerCase())) {
                    matches.add(option);
                }
            }
            return matches;
        }
        return List.of();
    }
}
