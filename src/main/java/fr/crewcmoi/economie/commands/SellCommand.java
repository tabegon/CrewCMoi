package fr.crewcmoi.economie.commands;

import fr.crewcmoi.economie.gui.SellGuiManager;
import fr.crewcmoi.economie.managers.PricesManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Commande /sell désactivée : la vente ne se déclenche plus via cette
 * commande mais via un clic droit sur le NPC Citizens dédié (voir
 * {@link fr.crewcmoi.economie.listeners.SellNpcListener}).
 *
 * Sous-commande /sell reload (OP uniquement) : recharge prices.yml à chaud,
 * pour que l'émeraude de vente affichée chez le NPC reflète immédiatement
 * les nouveaux prix sans avoir à redémarrer le serveur.
 */
public class SellCommand implements CommandExecutor {

    private final SellGuiManager sellGuiManager;
    private final PricesManager pricesManager;

    public SellCommand(SellGuiManager sellGuiManager, PricesManager pricesManager) {
        this.sellGuiManager = sellGuiManager;
        this.pricesManager = pricesManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.isOp() && !sender.hasPermission("crewcmoi.sell.reload")) {
                sender.sendMessage(ChatColor.RED + "Tu n'as pas la permission d'utiliser cette commande.");
                return true;
            }
            pricesManager.reload();
            sender.sendMessage(ChatColor.GREEN + "prices.yml rechargé, les prix de vente sont à jour.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur.");
            return true;
        }

        player.sendMessage(ChatColor.RED + "Cette commande est désactivée. Rendez-vous chez le PNJ dédié et faites un clic droit dessus pour vendre vos objets.");
        return true;
    }
}
