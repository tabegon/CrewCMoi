package fr.crewcmoi.commands;

import fr.crewcmoi.gui.SellGuiManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Commande /sell : ouvre la GUI de vente d'objets.
 */
public class SellCommand implements CommandExecutor {

    private final SellGuiManager sellGuiManager;

    public SellCommand(SellGuiManager sellGuiManager) {
        this.sellGuiManager = sellGuiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur.");
            return true;
        }

        sellGuiManager.open(player);
        return true;
    }
}
