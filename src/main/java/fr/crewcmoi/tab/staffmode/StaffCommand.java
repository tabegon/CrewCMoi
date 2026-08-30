package fr.crewcmoi.tab.staffmode;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /staff : bascule le membre du staff entre son état normal (inventaire
 * habituel, vrai rôle affiché dans le tab) et son état incognito (inventaire
 * dédié au staff, affiché comme "Player" dans le tab). Voir StaffModeManager.
 * Réservé aux vrais rôles Fonda/Admin/Dev/Mod (Vip/Player ne peuvent pas
 * l'utiliser, même avec un rôle manuel via /role set).
 */
public class StaffCommand implements CommandExecutor {

    private final StaffModeManager staffModeManager;

    public StaffCommand(StaffModeManager staffModeManager) {
        this.staffModeManager = staffModeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande n'est utilisable qu'en jeu.");
            return true;
        }

        StaffModeManager.ToggleResult result = staffModeManager.toggle(player);
        switch (result) {
            case NOT_ALLOWED -> player.sendMessage(ChatColor.RED + "Cette commande est réservée au staff (Fonda/Admin/Dev/Mod).");
            case NOW_STAFF -> player.sendMessage(ChatColor.GOLD + "Mode staff activé : inventaire dédié, tu apparais comme un joueur normal dans le tab.");
            case NOW_NORMAL -> player.sendMessage(ChatColor.GREEN + "Mode staff désactivé : tu as retrouvé ton inventaire et ton rôle habituels.");
        }
        return true;
    }
}
