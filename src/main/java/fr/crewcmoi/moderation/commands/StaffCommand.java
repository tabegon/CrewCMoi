package fr.crewcmoi.moderation.commands;
import fr.crewcmoi.other.utils.Messages;

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
            Messages.send(sender, "server.staff-f9476f5");
            return true;
        }

        StaffModeManager.ToggleResult result = staffModeManager.toggle(player);
        switch (result) {
            case NOT_ALLOWED -> Messages.send(player, "server.staff-af68c90");
            case NOW_STAFF -> Messages.send(player, "server.staff-534bef4");
            case NOW_NORMAL -> Messages.send(player, "server.staff-cefa9a6");
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
            Messages.send(sender, "server.staff-1091a9a");
            return;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                Messages.send(sender, "server.staff-36f20e5");
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            Messages.send(sender, "server.staff-997c31f");
            return;
        }

        Role realRole = roleManager.getRealRole(target);
        boolean staffActive = staffModeManager.isActive(target.getUniqueId());
        boolean vanished = vanishManager.isVanished(target.getUniqueId());

        Messages.send(sender, "server.staff-info-title", java.util.Map.of("player", target.getName()));
        Messages.send(sender, "server.staff-info-role", java.util.Map.of("role", roleManager.getPrefix(realRole).trim()));
        Messages.send(sender, "server.staff-info-mode", java.util.Map.of("status", staffActive ? "&aactivé" : "&cdésactivé"));
        Messages.send(sender, "server.staff-info-vanish", java.util.Map.of("status", vanished ? "&aactivé" : "&cdésactivé"));
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
