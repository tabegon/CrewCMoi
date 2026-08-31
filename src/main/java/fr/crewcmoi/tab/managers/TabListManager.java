package fr.crewcmoi.tab.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.tab.roles.Role;
import fr.crewcmoi.tab.roles.RoleManager;
import org.bukkit.entity.Player;

/**
 * Applique le rôle d'un joueur (voir RoleManager) à l'affichage du tab : préfixe
 * coloré + tri dans le tab (les rôles à plus fort poids, comme Fonda ou Admin,
 * apparaissent en haut de la liste).
 * <p>
 * Ne manipule aucune team directement : le préfixe est délégué à
 * {@link PlayerTeamManager}, qui gère une team unique par joueur partagée avec
 * les autres fonctionnalités qui en ont besoin (suffixe de prime, masquage du
 * pseudo pendant l'invisibilité...) — voir sa Javadoc pour le détail du problème
 * que ça résout.
 */
public class TabListManager {

    private final Main plugin;
    private final RoleManager roleManager;
    private final PlayerTeamManager playerTeamManager;

    public TabListManager(Main plugin, RoleManager roleManager, PlayerTeamManager playerTeamManager) {
        this.plugin = plugin;
        this.roleManager = roleManager;
        this.playerTeamManager = playerTeamManager;
    }

    /**
     * (Re)calcule le rôle du joueur et met à jour le préfixe + le tri de sa
     * team personnelle en conséquence.
     */
    public void applyRole(Player player) {
        Role role = roleManager.getRole(player);
        playerTeamManager.setPrefix(player, roleManager.getPrefix(role), role.getWeight());
    }

    /**
     * À appeler à la connexion d'un joueur : applique son propre rôle, puis
     * s'assure que sa scoreboard voit bien l'état (rôle/prime/invisibilité) de
     * tous les autres joueurs déjà suivis (voir PlayerTeamManager#refreshOnJoin).
     */
    public void onPlayerJoin(Player player) {
        applyRole(player);
        playerTeamManager.refreshOnJoin(player);
    }

    /**
     * Recalcule le rôle et réapplique la team de TOUS les joueurs en ligne (ex:
     * après un /rank reload, ou un changement de préfixe en config.yml).
     */
    public void refreshAll() {
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            applyRole(online);
        }
    }
}
