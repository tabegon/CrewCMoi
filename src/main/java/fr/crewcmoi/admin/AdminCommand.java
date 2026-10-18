package fr.crewcmoi.admin;

import fr.crewcmoi.claims.managers.ClaimManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;

public final class AdminCommand implements CommandExecutor {
    private final AdminGuiManager guiManager;
    private final ClaimManager claimManager;

    public AdminCommand(AdminGuiManager guiManager, ClaimManager claimManager) {
        this.guiManager = guiManager;
        this.claimManager = claimManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Cette commande est réservée aux joueurs."); return true; }
        if (!player.hasPermission("crew.admin") && !player.hasPermission("crew.claim.admin")) {
            player.sendMessage(ChatColor.RED + "Tu n'as pas la permission.");
            return true;
        }
        if (args.length == 0) { guiManager.open(player); return true; }
        if (!args[0].equalsIgnoreCase("zone") || (!player.hasPermission("crew.claim.admin") && !player.hasPermission("crew.admin"))) {
            player.sendMessage(ChatColor.YELLOW + "Usage : /admin ou /admin zone pos1|pos2|add <nom>|remove <nom>|list");
            return true;
        }
        if (args.length < 2) { player.sendMessage(ChatColor.YELLOW + "/admin zone pos1|pos2|add <nom>|remove <nom>|list"); return true; }
        switch (args[1].toLowerCase()) {
            case "pos1" -> { claimManager.setZoneCorner(player, 1); player.sendMessage(ChatColor.GREEN + "Point 1 de la zone défini."); }
            case "pos2" -> { claimManager.setZoneCorner(player, 2); player.sendMessage(ChatColor.GREEN + "Point 2 de la zone défini."); }
            case "add" -> {
                if (args.length < 3) { player.sendMessage(ChatColor.RED + "Usage : /admin zone add <nom>"); return true; }
                if (claimManager.addBlockedZone(player, args[2])) player.sendMessage(ChatColor.GREEN + "Zone interdite au claim enregistrée : " + args[2]);
                else player.sendMessage(ChatColor.RED + "Définis pos1 et pos2 dans le même monde avant d'ajouter la zone.");
            }
            case "remove" -> {
                if (args.length < 3) { player.sendMessage(ChatColor.RED + "Usage : /admin zone remove <nom>"); return true; }
                player.sendMessage(claimManager.removeBlockedZone(args[2]) ? ChatColor.GREEN + "Zone supprimée." : ChatColor.RED + "Zone introuvable.");
            }
            case "list" -> player.sendMessage(ChatColor.YELLOW + "Zones interdites : " + (claimManager.getBlockedZoneNames().isEmpty() ? "aucune" : claimManager.getBlockedZoneNames().stream().collect(Collectors.joining(", "))));
            default -> player.sendMessage(ChatColor.YELLOW + "/admin zone pos1|pos2|add <nom>|remove <nom>|list");
        }
        return true;
    }
}
