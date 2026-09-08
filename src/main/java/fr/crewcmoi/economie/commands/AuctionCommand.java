package fr.crewcmoi.economie.commands;
import fr.crewcmoi.other.utils.Messages;

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
            Messages.send(sender, "economy.auction.player-only");
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

        Messages.send(player, "economy.auction.usage");
        return true;
    }

    private void handleSell(Player player, String[] args) {
        if (args.length < 2) {
            Messages.send(player, "economy.auction.sell-usage");
            return;
        }

        double price;
        try {
            price = MoneyFormat.parse(args[1]);
        } catch (NumberFormatException e) {
            Messages.send(player, "economy.auction.invalid-price");
            return;
        }

        double minPrice = plugin.getConfig().getDouble("economy.auction-min-price", 0.01);
        if (price < minPrice) {
            Messages.send(player, "economy.auction.min-price", java.util.Map.of("amount", MoneyFormat.format(minPrice)));
            return;
        }

        int maxSlots = auctionManager.getMaxSlots();
        if (auctionManager.countActiveAuctions(player.getUniqueId()) >= maxSlots) {
            Messages.send(player, "economy.auction.max-slots", java.util.Map.of("max", String.valueOf(maxSlots)));
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            Messages.send(player, "economy.auction.no-item");
            return;
        }

        ItemStack toSell = hand.clone();
        player.getInventory().setItemInMainHand(null);

        auctionManager.listItem(player, toSell, price, success -> {
            if (Boolean.TRUE.equals(success)) {
                String currency = plugin.getConfig().getString("economy.currency-symbol");
                Messages.send(player, "economy.auction.listed", java.util.Map.of("amount", MoneyFormat.format(price) + currency));
            } else {
                
                player.getInventory().addItem(toSell);
                Messages.send(player, "economy.auction.list-failed");
            }
        });
    }

    private void sendMessage(Player player, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
