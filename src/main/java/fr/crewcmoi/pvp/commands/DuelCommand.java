package fr.crewcmoi.pvp.commands;
import fr.crewcmoi.other.utils.Messages;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.gui.DuelConfigGuiManager;
import fr.crewcmoi.pvp.managers.CombatManager;
import fr.crewcmoi.pvp.managers.DuelManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
            Messages.send(sender, "duel.command.player-only");
            return true;
        }

        Player requester = (Player) sender;

        if (args.length != 1) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix") +
                            plugin.getMessages().getString("duel.usage")));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix") +
                            plugin.getMessages().getString("general.player-not-found")));
            return true;
        }

        if (target.getUniqueId().equals(requester.getUniqueId())) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix") +
                            plugin.getMessages().getString("duel.self")));
            return true;
        }

        if (duelManager.isInDuel(requester.getUniqueId()) || duelManager.isInDuel(target.getUniqueId())) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix") +
                            plugin.getMessages().getString("duel.already-in-duel")));
            return true;
        }

        if (combatManager != null && combatManager.isInCombat(requester)) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix") +
                            plugin.getMessages().getString("combat-log.action-blocked")
                                    .replace("{seconds}", String.valueOf(combatManager.getRemainingSeconds(requester)))));
            return true;
        }

        duelConfigGuiManager.open(requester, target);
        return true;
    }
}
