package fr.crewcmoi.moderation.commands;
import fr.crewcmoi.other.utils.Messages;

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
            Messages.send(sender, "server.vanish-f9476f5");
            return true;
        }

        if (!staffModeManager.isActive(player.getUniqueId())) {
            Messages.send(player, "server.vanish-1b9ce3e");
            return true;
        }

        boolean nowVanished = vanishManager.toggle(player);
        if (nowVanished) {
            Messages.send(player, "server.vanish-39e9b4b");
        } else {
            Messages.send(player, "server.vanish-c8ea56d");
        }
        return true;
    }
}
