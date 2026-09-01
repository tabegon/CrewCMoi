package fr.crewcmoi.moderation.commands;

import fr.crewcmoi.moderation.managers.StaffModeManager;
import fr.crewcmoi.moderation.managers.VanishManager;
import fr.crewcmoi.tab.roles.Role;
import fr.crewcmoi.tab.roles.RoleManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /staff :
 *  - /staff              : bascule entre l'état normal (inventaire habituel,
 *    façade Vip dans le tab) et l'état staff (inventaire dédié, vrai rôle
 *    affiché — voir StaffModeManager). Réservé aux vrais rôles Fonda/Admin/
 *    Dev/Mod (Vip/Player ne peuvent pas l'utiliser, même avec un rôle manuel
 *    via /rank set).
 *  - /staff info [joueur] : affiche le vrai rôle, l'état du mode staff et du
 *    vanish d'un joueur (soi-même par défaut). Réservé aux OP et aux vrais
 *    rôles de staff.
 */
public class StaffCommand implements CommandExecutor, TabCompleter {

    private final StaffModeManager staffModeManager;
    private final RoleManager roleManager;
    private final VanishManager vanishManager;

    public StaffCommand(StaffModeManager staffModeManager, RoleManager roleManager, VanishManager vanishManager) {
        this.staffModeManager = staffModeManager;
        this.roleManager = roleManager;
        this.vanishManager = vanishManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("info")) {
            handleInfo(sender, args);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande n'est utilisable qu'en jeu.");
            return true;
        }

        StaffModeManager.ToggleResult result = staffModeManager.toggle(player);
        switch (result) {
            case NOT_ALLOWED -> player.sendMessage(ChatColor.RED + "Cette commande est réservée au staff (Fonda/Admin/Dev/Mod).");
            case NOW_STAFF -> player.sendMessage(ChatColor.GOLD + "Mode staff activé : inventaire dédié, ton vrai rôle est maintenant visible dans le tab.");
            case NOW_NORMAL -> player.sendMessage(ChatColor.GREEN + "Mode staff désactivé : tu as retrouvé ton inventaire habituel, et tu apparais comme Vip dans le tab.");
        }
        return true;
    }

    /**
     * /staff info [joueur] : réservé aux OP et aux vrais rôles de staff (même si
     * affichés comme Vip via la façade). Affiche le vrai rôle, l'état du mode
     * staff et du vanish de la cible (soi-même par défaut).
     */
    private void handleInfo(CommandSender sender, String[] args) {
        boolean allowed = sender.isOp() || (sender instanceof Player p && roleManager.getRealRole(p).isStaffRole());
        if (!allowed) {
            sender.sendMessage(ChatColor.RED + "Tu n'as pas la permission d'utiliser cette commande.");
            return;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Joueur introuvable ou hors ligne.");
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(ChatColor.RED + "Précise un joueur : /staff info <joueur>");
            return;
        }

        Role realRole = roleManager.getRealRole(target);
        boolean staffActive = staffModeManager.isActive(target.getUniqueId());
        boolean vanished = vanishManager.isVanished(target.getUniqueId());

        sender.sendMessage(ChatColor.GRAY + "--- Infos staff de " + ChatColor.YELLOW + target.getName() + ChatColor.GRAY + " ---");
        sender.sendMessage(ChatColor.GRAY + "Vrai rang : " + roleManager.getPrefix(realRole).trim());
        sender.sendMessage(ChatColor.GRAY + "Mode staff : " + (staffActive ? ChatColor.GREEN + "activé" : ChatColor.RED + "désactivé"));
        sender.sendMessage(ChatColor.GRAY + "Vanish : " + (vanished ? ChatColor.GREEN + "activé" : ChatColor.RED + "désactivé"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("info");
            return filter(options, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return filter(names, args[1]);
        }
        return new ArrayList<>();
    }

    private List<String> filter(List<String> options, String partial) {
        String lower = partial.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
