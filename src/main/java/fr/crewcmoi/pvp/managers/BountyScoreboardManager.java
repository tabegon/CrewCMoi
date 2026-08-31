package fr.crewcmoi.pvp.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.tab.managers.PlayerTeamManager;
import fr.crewcmoi.utils.MoneyFormat;
import org.bukkit.ChatColor;

import java.util.UUID;

/**
 * Affiche le montant de la prime active d'un joueur en suffixe de son pseudo
 * dans le tab (ex: "Steve  250 ⤴").
 * <p>
 * Ne gère plus sa propre team scoreboard : le suffixe est délégué à
 * {@link PlayerTeamManager}, qui gère une team unique par joueur partagée avec
 * le rôle affiché (préfixe) et l'invisibilité (masquage du pseudo), pour que
 * les trois restent compatibles entre eux (voir la Javadoc de PlayerTeamManager).
 */
public class BountyScoreboardManager {

    private final Main plugin;
    private final PlayerTeamManager playerTeamManager;

    public BountyScoreboardManager(Main plugin, PlayerTeamManager playerTeamManager) {
        this.plugin = plugin;
        this.playerTeamManager = playerTeamManager;
    }

    /**
     * Met à jour (ou retire, si total <= 0) le suffixe de prime affiché à côté du
     * pseudo de ce joueur, pour tout le monde — fonctionne même s'il est hors
     * ligne, le suffixe s'appliquera dès sa reconnexion.
     */
    public void update(UUID targetUuid, String targetName, double total) {
        if (total <= 0) {
            remove(targetUuid);
            return;
        }

        String suffix = ChatColor.GOLD + " [" + MoneyFormat.format(total) + " " + ChatColor.WHITE + "\uE517" + ChatColor.GOLD + "]";
        playerTeamManager.setSuffix(targetUuid, targetName, suffix);
    }

    /** Supprime le suffixe de prime d'un joueur (prime retombée à 0, ou réclamée/déconnexion). */
    public void remove(UUID targetUuid) {
        playerTeamManager.clearSuffix(targetUuid);
    }
}
