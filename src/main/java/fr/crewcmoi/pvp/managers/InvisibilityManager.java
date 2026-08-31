package fr.crewcmoi.pvp.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.tab.managers.PlayerTeamManager;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * Gère l'anonymat des joueurs sous l'effet d'invisibilité (potion) :
 *  - masque leur pseudo (nametag) au-dessus de la tête, pour tout le monde, tant que
 *    l'effet est actif ;
 *  - permet de savoir si un joueur doit être traité comme "anonyme" au moment d'un
 *    kill, pour masquer son nom dans le message de mort diffusé au serveur (voir
 *    BountyListener#onPlayerDeathAnonymizeInvisibleKiller). Il reste malgré tout
 *    capable de toucher les primes normalement (voir BountyListener#claimBounty) :
 *    seul son identité affichée aux autres joueurs change, pas ses gains.
 * <p>
 * Le masquage passe par {@link PlayerTeamManager}, qui ne touche qu'à la
 * visibilité du pseudo sur la team personnelle du joueur, sans jamais écraser
 * son préfixe de rôle ni son suffixe de prime (voir la Javadoc de
 * PlayerTeamManager).
 */
public class InvisibilityManager {

    public static final String ANONYMOUS_NAME = "Anonyme";

    private final Main plugin;
    private final PlayerTeamManager playerTeamManager;

    public InvisibilityManager(Main plugin, PlayerTeamManager playerTeamManager) {
        this.plugin = plugin;
        this.playerTeamManager = playerTeamManager;
    }

    public void hideNameTag(Player player) {
        playerTeamManager.setNameTagHidden(player, true);
    }

    public void showNameTag(Player player) {
        playerTeamManager.setNameTagHidden(player, false);
    }

    /**
     * Un joueur est considéré "anonyme" au moment d'un kill s'il a l'effet
     * d'invisibilité actif (potion) : son pseudo doit alors être masqué dans le
     * message de mort diffusé à tout le serveur.
     */
    public boolean isAnonymous(Player player) {
        return player != null && player.hasPotionEffect(PotionEffectType.INVISIBILITY);
    }
}
