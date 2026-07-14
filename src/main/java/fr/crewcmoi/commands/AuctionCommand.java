package fr.crewcmoi.commands;

import fr.crewcmoi.Main;
import fr.crewcmoi.gui.AuctionGuiManager;
import fr.crewcmoi.managers.AuctionManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;

/**
 * Commande /ah (hôtel des ventes) :
 *  - /ah              : ouvre la GUI de parcours des annonces.
 *  - /ah sell <prix>  : met en vente l'objet tenu en main.
 */
public class AuctionCommand implements CommandExecutor {

    private final Main plugin;
    private final AuctionManager auctionManager;
    private final AuctionGuiManager auctionGuiManager;
    private final DecimalFormat format = new DecimalFormat("#,##0.00");

    public AuctionCommand(Main plugin, AuctionManager auctionManager, AuctionGuiManager auctionGuiManager) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
        this.auctionGuiManager = auctionGuiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur.");
            return true;
        }

        if (args.length == 0) {
            auctionGuiManager.open(player, 0);
            return true;
        }

        if (args[0].equalsIgnoreCase("sell")) {
            handleSell(player, args);
            return true;
        }

        sendMessage(player, "&cUsage : /ah &7ou&c /ah sell <prix>");
        return true;
    }

    private void handleSell(Player player, String[] args) {
        if (args.length < 2) {
            sendMessage(player, "&cUsage : /ah sell <prix>");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[1].replace(",", "."));
        } catch (NumberFormatException e) {
            sendMessage(player, "&cMontant invalide.");
            return;
        }

        double minPrice = plugin.getConfig().getDouble("economy.auction-min-price", 0.01);
        if (price < minPrice) {
            sendMessage(player, "&cLe prix minimum est de &e" + format.format(minPrice) + "&c.");
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            sendMessage(player, "&cVous devez tenir un objet en main pour le mettre en vente.");
            return;
        }

        ItemStack toSell = hand.clone();
        player.getInventory().setItemInMainHand(null);

        auctionManager.listItem(player, toSell, price, success -> {
            if (Boolean.TRUE.equals(success)) {
                String currency = plugin.getConfig().getString("economy.currency-symbol", "$");
                sendMessage(player, "&aObjet mis en vente pour &e" + format.format(price) + currency + "&a !");
            } else {
                // Échec : on rend l'objet au joueur.
                player.getInventory().addItem(toSell);
                sendMessage(player, "&cUne erreur est survenue, l'objet vous a été rendu.");
            }
        });
    }

    private void sendMessage(Player player, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&6Economy&8] &r" + message));
    }
}
