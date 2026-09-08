package fr.crewcmoi.economie.commands;
import fr.crewcmoi.other.utils.Messages;

import fr.crewcmoi.economie.gui.SellGuiManager;
import fr.crewcmoi.economie.managers.PricesManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
                Messages.send(sender, "economy.sell.no-permission");
                return true;
            }
            pricesManager.reload();
            Messages.send(sender, "economy.sell.prices-reloaded");
            return true;
        }

        if (!(sender instanceof Player player)) {
            Messages.send(sender, "economy.sell.player-only");
            return true;
        }

        Messages.send(player, "economy.sell.disabled");
        return true;
    }
}
