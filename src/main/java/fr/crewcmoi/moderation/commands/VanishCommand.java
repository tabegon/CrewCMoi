package fr.crewcmoi.moderation.commands;

import fr.crewcmoi.moderation.managers.StaffModeManager;
import fr.crewcmoi.moderation.managers.VanishManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /vanish : bascule le vanish (voir VanishManager) — invisibilité totale,
 * armure comprise, et disparition du tab, comme si le joueur n'était plus
 * connecté. Réservé au mode staff (/staff) : inutilisable en dehors.
 */
public class VanishCommand implements CommandExecutor {

    private final StaffModeManager staffModeManager;
    private final VanishManager vanishManager;

    public VanishCommand(StaffModeManager staffModeManager, VanishManager vanishManager) {
        this.staffModeManager = staffModeManager;
        this.vanishManager = vanishManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande n'est utilisable qu'en jeu.");
            return true;
        }

        if (!staffModeManager.isActive(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Tu dois être en mode staff (/staff) pour utiliser /vanish.");
            return true;
        }

        boolean nowVanished = vanishManager.toggle(player);
        if (nowVanished) {
            player.sendMessage(ChatColor.GOLD + "Vanish activé : tu es totalement invisible et absent du tab.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Vanish désactivé : tu es de nouveau visible.");
        }
        return true;
    }
}
