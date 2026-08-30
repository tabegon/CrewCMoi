package fr.crewcmoi.economie.commands;

import fr.crewcmoi.economie.gui.SellGuiManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Commande /sell désactivée : la vente ne se déclenche plus via cette
 * commande mais via un clic droit sur le NPC Citizens dédié (voir
 * {@link fr.crewcmoi.economie.listeners.SellNpcListener}).
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

        player.sendMessage(ChatColor.RED + "Cette commande est désactivée. Rendez-vous chez le PNJ dédié et faites un clic droit dessus pour vendre vos objets.");
        return true;
    }
}
