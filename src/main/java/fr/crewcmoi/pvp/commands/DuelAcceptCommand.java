package fr.crewcmoi.pvp.commands;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.managers.DuelManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Commande /duelaccept : accepte la demande de duel reçue. Avec l'argument "deny"
 * (ou via le bouton [Refuser] cliquable), refuse la demande à la place.
 */
public class DuelAcceptCommand implements CommandExecutor {

    private final Main plugin;
    private final DuelManager duelManager;

    public DuelAcceptCommand(Main plugin, DuelManager duelManager) {
        this.plugin = plugin;
        this.duelManager = duelManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length >= 1 && args[0].equalsIgnoreCase("deny")) {
            duelManager.deny(player);
        } else {
            duelManager.accept(player);
        }
        return true;
    }
}
