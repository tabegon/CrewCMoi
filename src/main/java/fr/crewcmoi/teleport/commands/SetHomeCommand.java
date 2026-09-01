package fr.crewcmoi.teleport.commands;

import fr.crewcmoi.Main;
import fr.crewcmoi.teleport.managers.HomeManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Commande /sethome : enregistre la position actuelle du joueur comme son home.
 */
public class SetHomeCommand implements CommandExecutor {

    private final Main plugin;
    private final HomeManager homeManager;

    public SetHomeCommand(Main plugin, HomeManager homeManager) {
        this.plugin = plugin;
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur.");
            return true;
        }

        Player player = (Player) sender;

        homeManager.setHome(player, () -> player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getMessages().getString("prefix", "") +
                        plugin.getMessages().getString("home.set-success", "&aVotre home a été défini à votre position actuelle."))));

        return true;
    }
}
