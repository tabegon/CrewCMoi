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
            Messages.send(sender, "moderation.staff.player-only");
            return true;
        }

        StaffModeManager.ToggleResult result = staffModeManager.toggle(player);
        switch (result) {
            case NOT_ALLOWED -> Messages.send(player, "moderation.staff.staff-only");
            case NOW_STAFF -> Messages.send(player, "moderation.staff.enabled");
            case NOW_NORMAL -> Messages.send(player, "moderation.staff.disabled");
        }
        return true;
    }

    private void handleInfo(CommandSender sender, String[] args) {
        boolean allowed = sender.isOp() || (sender instanceof Player p && roleManager.getRealRole(p).isStaffRole());
        if (!allowed) {
            Messages.send(sender, "moderation.staff.no-permission");
            return;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                Messages.send(sender, "moderation.staff.player-not-found");
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            Messages.send(sender, "moderation.staff.info-usage");
            return;
        }

        Role realRole = roleManager.getRealRole(target);
        boolean staffActive = staffModeManager.isActive(target.getUniqueId());
        boolean vanished = vanishManager.isVanished(target.getUniqueId());

        Messages.send(sender, "moderation.staff.info-title", java.util.Map.of("player", target.getName()));
        Messages.send(sender, "moderation.staff.info-role", java.util.Map.of("role", roleManager.getPrefix(realRole).trim()));
        Messages.send(sender, "moderation.staff.info-mode", java.util.Map.of("status", staffActive ? "&aactivé" : "&cdésactivé"));
        Messages.send(sender, "moderation.staff.info-vanish", java.util.Map.of("status", vanished ? "&aactivé" : "&cdésactivé"));
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
