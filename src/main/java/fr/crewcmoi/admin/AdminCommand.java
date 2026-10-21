package fr.crewcmoi.admin;

import fr.crewcmoi.claims.managers.ClaimManager;
import fr.crewcmoi.Main;
import fr.crewcmoi.pets.PetManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;

public final class AdminCommand implements CommandExecutor {
    private final AdminGuiManager guiManager;
    private final ClaimManager claimManager;
    private final Main plugin;

    public AdminCommand(Main plugin, AdminGuiManager guiManager, ClaimManager claimManager) {
        this.plugin = plugin;
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
        if (args[0].equalsIgnoreCase("pets")) {
            if (!player.hasPermission("crew.admin") && !player.isOp()) {
                player.sendMessage(ChatColor.RED + "Tu n'as pas la permission.");
                return true;
            }
            if (args.length < 3) {
                player.sendMessage(ChatColor.YELLOW + "Usage : /admin pets <joueur> <pet|all|1|2>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) { player.sendMessage(ChatColor.RED + "Le joueur doit être connecté."); return true; }
            PetManager pets = plugin.getPetManager();
            if (pets == null) { player.sendMessage(ChatColor.RED + "Le système de pets est indisponible."); return true; }
            String choice = args[2];
            if (choice.equalsIgnoreCase("all")) {
                int removed = pets.resetOwnedPets(target.getUniqueId(), Integer.MAX_VALUE);
                player.sendMessage(ChatColor.GREEN + "" + removed + " pet(s) retiré(s) à " + target.getName() + ".");
            } else if (choice.equals("1") || choice.equals("2")) {
                int removed = pets.resetOwnedPets(target.getUniqueId(), Integer.parseInt(choice));
                player.sendMessage(ChatColor.GREEN + "" + removed + " pet(s) retiré(s) à " + target.getName() + ".");
            } else if (pets.removeOwnedPet(target.getUniqueId(), choice)) {
                player.sendMessage(ChatColor.GREEN + "Pet " + choice + " retiré à " + target.getName() + ".");
            } else {
                player.sendMessage(ChatColor.RED + "Ce joueur ne possède pas ce pet.");
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("quest")) {
            if (!player.hasPermission("crew.admin") && !player.isOp()) {
                player.sendMessage(ChatColor.RED + "Tu n'as pas la permission.");
                return true;
            }
            if (args.length < 3) {
                player.sendMessage(ChatColor.YELLOW + "Usage : /admin quest <joueur> <1|2|all>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) { player.sendMessage(ChatColor.RED + "Le joueur doit être connecté."); return true; }
            if (args[2].equalsIgnoreCase("all")) {
                plugin.getQuestManager().resetAllQuests(target);
            } else if (args[2].equals("1") || args[2].equals("2")) {
                plugin.getQuestManager().resetQuest(target, Integer.parseInt(args[2]));
            } else {
                player.sendMessage(ChatColor.YELLOW + "Usage : /admin quest <joueur> <1|2|all>");
                return true;
            }
            player.sendMessage(ChatColor.GREEN + "Progression des quêtes modifiée pour " + target.getName() + ".");
            return true;
        }
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
