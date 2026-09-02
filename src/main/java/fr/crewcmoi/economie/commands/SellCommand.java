package fr.crewcmoi.economie.commands;
import fr.crewcmoi.other.utils.Messages;

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
                Messages.send(sender, "server.sell-1091a9a");
                return true;
            }
            pricesManager.reload();
            Messages.send(sender, "server.sell-a1b999f");
            return true;
        }

        if (!(sender instanceof Player player)) {
            Messages.send(sender, "server.sell-8660550");
            return true;
        }

        Messages.send(player, "server.sell-c3c6159");
        return true;
    }
}
