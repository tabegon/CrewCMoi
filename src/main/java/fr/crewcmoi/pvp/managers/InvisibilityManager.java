package fr.crewcmoi.pvp.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.tab.managers.TabListManager;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Gère l'anonymat des joueurs sous l'effet d'invisibilité (potion) :
 *  - masque leur pseudo (nametag) au-dessus de la tête, pour tout le monde, tant que
 *    l'effet est actif, via une team scoreboard dédiée dont le nametag est caché ;
 *  - permet de savoir si un joueur doit être traité comme "anonyme" au moment d'un
 *    kill, pour masquer son nom dans le message de mort diffusé au serveur (voir
 *    BountyListener#onPlayerDeathAnonymizeInvisibleKiller). Il reste malgré tout
 *    capable de toucher les primes normalement (voir BountyListener#claimBounty) :
 *    seul son identité affichée aux autres joueurs change, pas ses gains.
 */
public class InvisibilityManager {

    private static final String TEAM_NAME = "cc_invisible";
    public static final String ANONYMOUS_NAME = "Anonyme";

    private final Main plugin;

    // Optionnel : si renseigné (voir setTabListManager), permet de réappliquer la
    // team de rôle (préfixe/tri du tab, package fr.crewcmoi.tab) d'un joueur dès
    // que son invisibilité se termine, puisqu'un joueur ne peut appartenir qu'à
    // une seule team scoreboard à la fois (la sienne, ou "cc_invisible").
    private TabListManager tabListManager;

    public InvisibilityManager(Main plugin) {
        this.plugin = plugin;
    }

    public void setTabListManager(TabListManager tabListManager) {
        this.tabListManager = tabListManager;
    }

    public void hideNameTag(Player player) {
        Team team = getOrCreateTeam();
        Scoreboard scoreboard = team.getScoreboard();

        // Retire le joueur de sa team actuelle, quelle qu'elle soit (ex: sa team de
        // rôle/tab), avant de l'ajouter à "cc_invisible" : un joueur ne peut
        // appartenir qu'à une seule team à la fois sur un même scoreboard.
        Team current = scoreboard.getEntryTeam(player.getName());
        if (current != null && current != team) {
            current.removeEntry(player.getName());
        }
        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
    }

    public void showNameTag(Player player) {
        Team team = getOrCreateTeam();
        if (team.hasEntry(player.getName())) {
            team.removeEntry(player.getName());
        }
        // Réapplique sa team de rôle (préfixe/tri dans le tab) maintenant qu'il
        // n'est plus masqué, voir fr.crewcmoi.tab.managers.TabListManager.
        if (tabListManager != null) {
            tabListManager.applyRole(player);
        }
    }

    /**
     * Un joueur est considéré "anonyme" au moment d'un kill s'il a l'effet
     * d'invisibilité actif (potion) : son pseudo doit alors être masqué dans le
     * message de mort diffusé à tout le serveur.
     */
    public boolean isAnonymous(Player player) {
        return player != null && player.hasPotionEffect(PotionEffectType.INVISIBILITY);
    }

    private Team getOrCreateTeam() {
        Scoreboard scoreboard = plugin.getServer().getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(TEAM_NAME);
        if (team == null) {
            team = scoreboard.registerNewTeam(TEAM_NAME);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        }
        return team;
    }
}
