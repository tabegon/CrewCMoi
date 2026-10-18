package fr.crewcmoi.economie.commands;

import fr.crewcmoi.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ResetQuestCommand implements CommandExecutor {
    private final Main plugin;

    public ResetQuestCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("crewcmoi.admin.resetquest")) {
            sender.sendMessage(ChatColor.RED + "Tu n'as pas la permission.");
            return true;
        }
        if (args.length != 2 || !args[0].equalsIgnoreCase("quest")) {
            sender.sendMessage(ChatColor.RED + "Usage : /reset quest <joueur>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Ce joueur doit être connecté pour réinitialiser ses quêtes.");
            return true;
        }
        plugin.getQuestManager().resetAllQuests(target);
        sender.sendMessage(ChatColor.GREEN + "Toutes les quêtes de " + target.getName() + " ont été réinitialisées.");
        return true;
    }
}
