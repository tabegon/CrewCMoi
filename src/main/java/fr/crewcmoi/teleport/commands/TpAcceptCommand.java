package fr.crewcmoi.teleport.commands;
import fr.crewcmoi.other.utils.Messages;

import fr.crewcmoi.Main;
import fr.crewcmoi.teleport.managers.TeleportManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Commande /tpaccept : accepte la dernière demande de téléportation reçue (/tpa ou /tpahere).
 * Peut aussi être déclenchée via le bouton [Teleporter] cliquable dans le chat.
 */
public class TpAcceptCommand implements CommandExecutor {

    private final Main plugin;
    private final TeleportManager teleportManager;

    public TpAcceptCommand(Main plugin, TeleportManager teleportManager) {
        this.plugin = plugin;
        this.teleportManager = teleportManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Messages.send(sender, "server.tpaccept-8660550");
            return true;
        }

        teleportManager.accept((Player) sender);
        return true;
    }
}
