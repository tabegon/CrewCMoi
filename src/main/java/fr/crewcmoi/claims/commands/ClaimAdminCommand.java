package fr.crewcmoi.claims.commands;
import fr.crewcmoi.other.utils.Messages;

import fr.crewcmoi.claims.gui.ClaimAdminGuiManager;
import fr.crewcmoi.claims.managers.ClaimManager;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClaimAdminCommand implements CommandExecutor {

    private final ClaimAdminGuiManager guiManager;
    private final ClaimManager claimManager;

    public ClaimAdminCommand(ClaimAdminGuiManager guiManager, ClaimManager claimManager) {
        this.guiManager = guiManager;
        this.claimManager = claimManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "general.player-only");
            return true;
        }
        if (!player.hasPermission("crew.claim.admin")) {
            Messages.send(player, "general.no-permission");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("zone")) {
            if (args.length < 2) {
                player.sendMessage("§e/claimadmin zone pos1|pos2|add <nom>|remove <nom>|list");
                return true;
            }
            switch (args[1].toLowerCase()) {
                case "pos1" -> { claimManager.setZoneCorner(player, 1); player.sendMessage("§aPoint 1 de la zone défini."); }
                case "pos2" -> { claimManager.setZoneCorner(player, 2); player.sendMessage("§aPoint 2 de la zone défini."); }
                case "add" -> {
                    if (args.length < 3) { player.sendMessage("§cUtilisation : /claimadmin zone add <nom>"); return true; }
                    if (claimManager.addBlockedZone(player, args[2])) player.sendMessage("§aZone interdite au claim enregistrée : §f" + args[2]);
                    else player.sendMessage("§cDéfinis pos1 et pos2 dans le même monde avant d'ajouter la zone.");
                }
                case "remove" -> {
                    if (args.length < 3) { player.sendMessage("§cUtilisation : /claimadmin zone remove <nom>"); return true; }
                    player.sendMessage(claimManager.removeBlockedZone(args[2]) ? "§aZone supprimée." : "§cZone introuvable.");
                }
                case "list" -> player.sendMessage("§eZones interdites : §f" + (claimManager.getBlockedZoneNames().isEmpty() ? "aucune" : claimManager.getBlockedZoneNames().stream().collect(Collectors.joining(", "))));
                default -> player.sendMessage("§e/claimadmin zone pos1|pos2|add <nom>|remove <nom>|list");
            }
            return true;
        }
        guiManager.open(player);
        return true;
    }
}
