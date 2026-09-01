package fr.crewcmoi.moderation.listeners;

import fr.crewcmoi.moderation.managers.StaffModeManager;
import fr.crewcmoi.tab.roles.RoleManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Si un joueur se déconnecte pendant qu'il est en mode /staff, ce listener
 * réapplique automatiquement l'état "mode staff actif" à sa reconnexion (pour
 * que son vrai rang redevienne visible plutôt que la façade Vip), avant que
 * {@link fr.crewcmoi.tab.listeners.TabListListener} (priorité MONITOR) n'applique
 * le rôle dans le tab — d'où la priorité NORMAL ici, pour s'exécuter avant.
 * <p>
 * Son inventaire "staff" est déjà celui avec lequel il s'est déconnecté (rien
 * à échanger ici), on ne fait que remettre à jour l'affichage du rôle.
 */
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
