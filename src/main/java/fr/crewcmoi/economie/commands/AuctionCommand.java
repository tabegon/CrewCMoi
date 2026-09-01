package fr.crewcmoi.economie.commands;

import fr.crewcmoi.Main;
import fr.crewcmoi.economie.gui.AuctionGuiManager;
import fr.crewcmoi.economie.managers.AuctionManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import fr.crewcmoi.other.utils.MoneyFormat;

/**
 * Commande /ah (hôtel des ventes) :
 *  - /ah              : ouvre la GUI de parcours des annonces.
 *  - /ah sell <prix>  : met en vente l'objet tenu en main.
 */
public class AuctionCommand implements CommandExecutor {

    private final Main plugin;
    private final AuctionManager auctionManager;
    private final AuctionGuiManager auctionGuiManager;

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

        sendMessage(player, "&cᴜꜱᴀɢᴇ : /ᴀʜ &7ᴏᴜ&c /ᴀʜ ꜱᴇʟʟ <ᴘʀɪx>");
        return true;
    }

    private void handleSell(Player player, String[] args) {
        if (args.length < 2) {
            sendMessage(player, "&cᴜꜱᴀɢᴇ : /ᴀʜ ꜱᴇʟʟ <ᴘʀɪx>");
            return;
        }

        double price;
        try {
            price = MoneyFormat.parse(args[1]);
        } catch (NumberFormatException e) {
            sendMessage(player, "&cᴍᴏɴᴛᴀɴᴛ ɪɴᴠᴀʟɪᴅᴇ.");
            return;
        }

        double minPrice = plugin.getConfig().getDouble("economy.auction-min-price", 0.01);
        if (price < minPrice) {
            sendMessage(player, "&cʟᴇ ᴘʀɪx ᴍɪɴɪᴍᴜᴍ ᴇꜱᴛ ᴅᴇ &e" + MoneyFormat.format(minPrice) + "&c.");
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            sendMessage(player, "&cᴠᴏᴜꜱ ᴅᴇᴠᴇᴢ ᴛᴇɴɪʀ ᴜɴ ᴏʙᴊᴇᴛ ᴇɴ ᴍᴀɪɴ ᴘᴏᴜʀ ʟᴇ ᴍᴇᴛᴛʀᴇ ᴇɴ ᴠᴇɴᴛᴇ.");
            return;
        }

        ItemStack toSell = hand.clone();
        player.getInventory().setItemInMainHand(null);

        auctionManager.listItem(player, toSell, price, success -> {
            if (Boolean.TRUE.equals(success)) {
                String currency = plugin.getConfig().getString("economy.currency-symbol", "§f");
                sendMessage(player, "&aᴏʙᴊᴇᴛ ᴍɪꜱ ᴇɴ ᴠᴇɴᴛᴇ ᴘᴏᴜʀ &e" + MoneyFormat.format(price) + currency + "&a !");
            } else {
                // Échec : on rend l'objet au joueur.
                player.getInventory().addItem(toSell);
                sendMessage(player, "&cᴜɴᴇ ᴇʀʀᴇᴜʀ ᴇꜱᴛ ꜱᴜʀᴠᴇɴᴜᴇ, ʟ'ᴏʙᴊᴇᴛ ᴠᴏᴜꜱ ᴀ ᴇᴛᴇ ʀᴇɴᴅᴜ.");
            }
        });
    }

    private void sendMessage(Player player, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&6ᴇᴄᴏɴᴏᴍʏ&8] &r" + message));
    }
}
