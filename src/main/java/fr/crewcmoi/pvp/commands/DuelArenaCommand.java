package fr.crewcmoi.pvp.commands;

import fr.crewcmoi.pvp.managers.DuelArenaManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Commande admin /duelarena : délimite le cuboïde de l'arène de duel (corner1/corner2),
 * sauvegarde son état propre (save) et permet de forcer une restauration manuelle (reset)
 * pour tester.
 *
 * Usage :
 *   /duelarena corner1   - pose le premier coin à la position du joueur
 *   /duelarena corner2   - pose le second coin à la position du joueur
 *   /duelarena save      - sauvegarde l'état actuel de la zone comme référence
 *   /duelarena reset     - restaure immédiatement la zone à l'état sauvegardé
 */
public class DuelArenaCommand implements CommandExecutor, TabCompleter {

    private final DuelArenaManager duelArenaManager;

    public DuelArenaCommand(DuelArenaManager duelArenaManager) {
        this.duelArenaManager = duelArenaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("crew.admin")) {
            sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("§cUsage : /duelarena <corner1|corner2|save|reset>");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "corner1", "corner2" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cSeul un joueur peut poser un coin (utilise ta position actuelle).");
                    return true;
                }
                int index = sub.equals("corner1") ? 1 : 2;
                duelArenaManager.setCorner(index, player.getLocation());
                sender.sendMessage("§aCoin " + index + " de l'arène de duel posé à ta position.");
                if (duelArenaManager.isRegionDefined()) {
                    sender.sendMessage("§7Zone définie. Décore l'arène puis fais §e/duelarena save§7.");
                }
            }
            case "save" -> duelArenaManager.saveSnapshot(sender);
            case "reset" -> {
                if (!duelArenaManager.hasSnapshot()) {
                    sender.sendMessage("§cAucune sauvegarde n'existe encore, fais d'abord /duelarena save.");
                    return true;
                }
                duelArenaManager.resetArena();
                sender.sendMessage("§aRestauration de l'arène en cours.");
            }
            default -> sender.sendMessage("§cUsage : /duelarena <corner1|corner2|save|reset>");
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
