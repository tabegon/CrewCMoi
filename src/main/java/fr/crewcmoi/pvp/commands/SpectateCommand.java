package fr.crewcmoi.pvp.commands;

import fr.crewcmoi.pvp.managers.SpectateManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SpectateCommand implements CommandExecutor, TabCompleter {
    private final SpectateManager manager;
    public SpectateCommand(SpectateManager manager) { this.manager = manager; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Cette commande doit être utilisée en jeu."); return true; }
        if (args.length == 1 && args[0].equalsIgnoreCase("stop")) { manager.stop(player, true); return true; }
        if (args.length != 1) { player.sendMessage(ChatColor.RED + "Usage : /spectate <joueur|stop>"); return true; }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { player.sendMessage(ChatColor.RED + "Joueur introuvable ou hors ligne."); return true; }
        manager.start(player, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(java.util.Locale.ROOT);
            if ("stop".startsWith(prefix)) result.add("stop");
            for (Player player : Bukkit.getOnlinePlayers()) if (player.getName().toLowerCase(java.util.Locale.ROOT).startsWith(prefix)) result.add(player.getName());
        }
        return result;
    }
}
