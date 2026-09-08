package fr.crewcmoi.moderation.listeners;

import fr.crewcmoi.moderation.managers.StaffModeManager;
import fr.crewcmoi.tab.roles.RoleManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class StaffModeListener implements Listener {

    private final StaffModeManager staffModeManager;
    private final RoleManager roleManager;

    public StaffModeListener(StaffModeManager staffModeManager, RoleManager roleManager) {
        this.staffModeManager = staffModeManager;
        this.roleManager = roleManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (staffModeManager.isActive(player.getUniqueId())) {
            roleManager.setStaffModeActive(player.getUniqueId(), true);
        }
    }
}
