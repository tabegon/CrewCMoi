package fr.crewcmoi.commands;

import fr.crewcmoi.Main;
import fr.crewcmoi.gui.DuelConfigGuiManager;
import fr.crewcmoi.managers.CombatManager;
import fr.crewcmoi.managers.DuelManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Commande /duel <joueur> : ouvre une GUI permettant de configurer les règles
 * du duel (keepinventory, argent en jeu) avant d'envoyer la demande au joueur ciblé.
 */
public class DuelCommand implements CommandExecutor {

    private final Main plugin;
    private final DuelManager duelManager;
    private final DuelConfigGuiManager duelConfigGuiManager;
    private final CombatManager combatManager;

    public DuelCommand(Main plugin, DuelManager duelManager, DuelConfigGuiManager duelConfigGuiManager,
                        CombatManager combatManager) {
        this.plugin = plugin;
        this.duelManager = duelManager;
        this.duelConfigGuiManager = duelConfigGuiManager;
        this.combatManager = combatManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur.");
            return true;
        }

        Player requester = (Player) sender;

        if (args.length != 1) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix", "") +
                            plugin.getMessages().getString("duel.usage", "&cUsage : /duel <joueur>")));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix", "") +
                            plugin.getMessages().getString("general.player-not-found", "&cCe joueur n'existe pas ou n'est pas en ligne.")));
            return true;
        }

        if (target.getUniqueId().equals(requester.getUniqueId())) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix", "") +
                            plugin.getMessages().getString("duel.self", "&cVous ne pouvez pas vous provoquer vous-même.")));
            return true;
        }

        if (duelManager.isInDuel(requester.getUniqueId()) || duelManager.isInDuel(target.getUniqueId())) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix", "") +
                            plugin.getMessages().getString("duel.already-in-duel", "&cCe joueur est déjà en duel.")));
            return true;
        }

        if (combatManager != null && combatManager.isInCombat(requester)) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix", "") +
                            plugin.getMessages().getString("combat-log.action-blocked", "&cVous ne pouvez pas faire ça en combat !")
                                    .replace("{seconds}", String.valueOf(combatManager.getRemainingSeconds(requester)))));
            return true;
        }

        duelConfigGuiManager.open(requester, target);
        return true;
    }
}
