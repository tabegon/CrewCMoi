package fr.crewcmoi.tab.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.tab.roles.Role;
import fr.crewcmoi.tab.roles.RoleManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class TabListManager {

    private final Main plugin;
    private final RoleManager roleManager;
    private final PlayerTeamManager playerTeamManager;

    public TabListManager(Main plugin, RoleManager roleManager, PlayerTeamManager playerTeamManager) {
        this.plugin = plugin;
        this.roleManager = roleManager;
        this.playerTeamManager = playerTeamManager;
    }

    public void applyHeader(Player player) {
        String header = plugin.getConfig().getString("tab.header", "\n    \n");
        String footer = plugin.getConfig().getString("tab.footer", "");
        player.setPlayerListHeaderFooter(
                ChatColor.translateAlternateColorCodes('&', header),
                ChatColor.translateAlternateColorCodes('&', footer)
        );
    }

    public void applyRole(Player player) {
        Role role = roleManager.getRole(player);
        playerTeamManager.setPrefix(player, roleManager.getPrefix(role), role.getWeight());
    }

    public void onPlayerJoin(Player player) {
        applyHeader(player);
        applyRole(player);
        playerTeamManager.refreshOnJoin(player);
    }

    public void refreshAll() {
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            applyHeader(online);
            applyRole(online);
        }
    }
}
