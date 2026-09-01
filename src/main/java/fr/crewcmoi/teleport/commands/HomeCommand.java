package fr.crewcmoi.teleport.commands;

import fr.crewcmoi.Main;
import fr.crewcmoi.teleport.managers.HomeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Commande /home : téléporte le joueur à son home précédemment défini via /sethome.
 */
public class HomeCommand implements CommandExecutor {

    private final Main plugin;
    private final HomeManager homeManager;

    public HomeCommand(Main plugin, HomeManager homeManager) {
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

        homeManager.getHome(player, home -> {
            if (home == null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getMessages().getString("prefix", "") +
                                plugin.getMessages().getString("home.no-home", "&cVous n'avez pas encore défini de home. Utilisez /sethome.")));
                return;
            }

            World world = Bukkit.getWorld(home.getWorld());
            if (world == null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getMessages().getString("prefix", "") +
                                plugin.getMessages().getString("home.world-missing", "&cLe monde de votre home est introuvable.")));
                return;
            }

            Location location = new Location(world, home.getX(), home.getY(), home.getZ(), home.getYaw(), home.getPitch());
            player.teleport(location);

            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix", "") +
                            plugin.getMessages().getString("home.teleported", "&aVous avez été téléporté à votre home.")));
        });

        return true;
    }
}
