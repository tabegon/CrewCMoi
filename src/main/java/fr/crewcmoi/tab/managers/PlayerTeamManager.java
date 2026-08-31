package fr.crewcmoi.tab.managers;

import fr.crewcmoi.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Point central qui gère UNE SEULE team scoreboard par joueur (sur la
 * scoreboard principale, ainsi que sur la scoreboard propre de chaque joueur
 * en ligne si elle diffère — voir onlineScoreboards()).
 * <p>
 * Bukkit n'autorise un joueur à être membre que d'UNE SEULE team par
 * scoreboard. Or plusieurs fonctionnalités du plugin ont besoin d'afficher
 * quelque chose via une team : le préfixe de rôle (voir TabListManager), le
 * suffixe de prime (voir BountyScoreboardManager), et le masquage du pseudo
 * pendant l'invisibilité (voir InvisibilityManager) — qui masque à la fois le
 * pseudo flottant au-dessus de la tête ET le préfixe/suffixe dans le tab (rang,
 * prime), pour qu'un joueur invisible reste vraiment anonyme partout. Les gérer chacune avec
 * leur propre team se marchait dessus : la dernière appliquée "volait"
 * l'entrée du joueur et faisait disparaître l'effet des autres (ex : un
 * joueur avec une prime active qui recevait son rôle dans le tab perdait le
 * suffixe de sa prime, ou inversement).
 * <p>
 * Ce manager centralise tout ça : chaque fonctionnalité ne fait que
 * déclarer son prefix/suffix/visibilité voulu pour un joueur, sans jamais
 * manipuler de team directement — c'est ce manager qui les combine sur la
 * team personnelle du joueur.
 */
public class PlayerTeamManager {

    private static final String TEAM_PREFIX = "ccp_";

    private final Main plugin;

    private final Map<Scoreboard, Map<UUID, Team>> teamsByScoreboard = new IdentityHashMap<>();

    private final Map<UUID, String> prefixes = new ConcurrentHashMap<>();
    private final Map<UUID, String> suffixes = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> hiddenNameTags = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> sortWeights = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();

    public PlayerTeamManager(Main plugin) {
        this.plugin = plugin;
    }

    /** Anciens préfixes de teams utilisés par des versions précédentes du plugin
     * (avant l'unification en une seule team par joueur) : nettoyés au démarrage
     * pour éviter des résidus s'ils ont persisté dans data/scoreboard.dat. */
    private static final String[] LEGACY_TEAM_PREFIXES = {"cc_role_", "cc_invisible", "ccbounty_", "r0_", "r1_", "r2_", "r3_", "r4_", "r5_"};

    /** Nettoie les teams orphelines d'une session précédente (ex : après un crash, ou une mise à jour du plugin). */
    public void start() {
        for (Scoreboard scoreboard : onlineScoreboards()) {
            for (Team team : scoreboard.getTeams().toArray(new Team[0])) {
                String name = team.getName();
                if (name.startsWith(TEAM_PREFIX) || startsWithAny(name, LEGACY_TEAM_PREFIXES)) {
                    team.unregister();
                }
            }
        }
    }

    private boolean startsWithAny(String value, String[] prefixes) {
        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public void stop() {
        for (Map<UUID, Team> teams : teamsByScoreboard.values()) {
            for (Team team : teams.values()) {
                if (team.getScoreboard() != null) {
                    team.unregister();
                }
            }
        }
        teamsByScoreboard.clear();
    }

    /**
     * Préfixe affiché devant le pseudo (ex : le rôle, "- Fonda "). sortWeight
     * détermine l'ordre dans le tab : plus il est petit, plus le joueur apparaît
     * haut (les teams étant triées par le client par ordre alphabétique de leur
     * nom, qui encode ce poids).
     */
    public void setPrefix(Player player, String prefix, int sortWeight) {
        UUID uuid = player.getUniqueId();
        names.put(uuid, player.getName());
        prefixes.put(uuid, prefix == null ? "" : prefix);
        sortWeights.put(uuid, sortWeight);
        applyToAllScoreboards(uuid);
    }

    /**
     * Suffixe affiché après le pseudo (ex : le montant de la prime). Fonctionne
     * même si le joueur ciblé est hors ligne (son nom est simplement pré-
     * enregistré comme entrée de sa team, et s'affichera à sa reconnexion).
     * Passer null pour retirer le suffixe.
     */
    public void setSuffix(UUID uuid, String targetName, String suffix) {
        names.put(uuid, targetName);
        if (suffix == null) {
            suffixes.remove(uuid);
        } else {
            suffixes.put(uuid, suffix);
        }
        applyToAllScoreboards(uuid);
    }

    /** Retire le suffixe d'un joueur (son nom doit déjà être connu, voir setPrefix/setSuffix). */
    public void clearSuffix(UUID uuid) {
        suffixes.remove(uuid);
        if (names.containsKey(uuid)) {
            applyToAllScoreboards(uuid);
        }
    }

    /**
     * Masque ou réaffiche, pour tout le monde, à la fois le pseudo au-dessus de
     * la tête du joueur ET son préfixe/suffixe dans le tab (rôle, prime) —
     * utilisé pendant l'invisibilité pour qu'il reste vraiment anonyme partout,
     * pas seulement dans le monde. Le rôle/la prime réels ne sont pas oubliés :
     * ils réapparaissent tels quels dès que hidden repasse à false.
     */
    public void setNameTagHidden(Player player, boolean hidden) {
        UUID uuid = player.getUniqueId();
        names.put(uuid, player.getName());
        hiddenNameTags.put(uuid, hidden);
        applyToAllScoreboards(uuid);
    }

    /**
     * Réapplique l'état déjà connu (prefix/suffix/visibilité) de TOUS les joueurs
     * suivis sur la scoreboard propre du joueur qui vient de se connecter (qui
     * peut être différente de la scoreboard principale, ex : si un autre plugin
     * lui en attribue une) : sans ça, son client ne verrait ni les rôles ni les
     * primes des autres joueurs tant qu'ils ne changent pas entre-temps. À
     * appeler depuis un PlayerJoinEvent (voir TabListManager#onPlayerJoin).
     */
    public void refreshOnJoin(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        for (UUID uuid : names.keySet()) {
            applyTo(scoreboard, uuid);
        }
    }

    /** Oublie totalement l'état d'un joueur (ex : à sa déconnexion). */
    public void clear(Player player) {
        UUID uuid = player.getUniqueId();
        prefixes.remove(uuid);
        suffixes.remove(uuid);
        hiddenNameTags.remove(uuid);
        sortWeights.remove(uuid);
        names.remove(uuid);
        for (Map<UUID, Team> teams : teamsByScoreboard.values()) {
            Team team = teams.remove(uuid);
            if (team != null && team.getScoreboard() != null) {
                team.unregister();
            }
        }
    }

    private void applyToAllScoreboards(UUID uuid) {
        for (Scoreboard scoreboard : onlineScoreboards()) {
            applyTo(scoreboard, uuid);
        }
    }

    private void applyTo(Scoreboard scoreboard, UUID uuid) {
        String entryName = names.get(uuid);
        if (scoreboard == null || entryName == null) {
            return;
        }

        int weight = sortWeights.getOrDefault(uuid, 99);
        String expectedName = teamName(uuid, weight);

        Map<UUID, Team> teams = teamsByScoreboard.computeIfAbsent(scoreboard, s -> new HashMap<>());
        Team team = teams.get(uuid);

        if (team != null && (team.getScoreboard() == null || !team.getName().equals(expectedName))) {
            // La team n'existe plus, ou le poids de tri a changé (ex : changement
            // de rôle) : Bukkit ne permettant pas de renommer une team, on la recrée.
            if (team.getScoreboard() != null) {
                team.unregister();
            }
            team = null;
        }

        if (team == null) {
            team = scoreboard.getTeam(expectedName);
            if (team == null) {
                team = scoreboard.registerNewTeam(expectedName);
            }
            teams.put(uuid, team);
        }

        if (!team.hasEntry(entryName)) {
            team.addEntry(entryName);
        }

        team.setPrefix(hiddenNameTags.getOrDefault(uuid, false) ? "" : cut(prefixes.getOrDefault(uuid, ""), 64));
        team.setSuffix(hiddenNameTags.getOrDefault(uuid, false) ? "" : cut(suffixes.getOrDefault(uuid, ""), 64));
        team.setOption(Team.Option.NAME_TAG_VISIBILITY,
                hiddenNameTags.getOrDefault(uuid, false) ? Team.OptionStatus.NEVER : Team.OptionStatus.ALWAYS);
    }

    /**
     * Toutes les scoreboards actuellement "en jeu" : la scoreboard principale,
     * ainsi que la scoreboard propre à chaque joueur en ligne si elle en utilise
     * une différente.
     */
    private Set<Scoreboard> onlineScoreboards() {
        Set<Scoreboard> scoreboards = Collections.newSetFromMap(new IdentityHashMap<>());
        scoreboards.add(Bukkit.getScoreboardManager().getMainScoreboard());
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            scoreboards.add(online.getScoreboard());
        }
        return scoreboards;
    }

    private String teamName(UUID uuid, int weight) {
        String compact = uuid.toString().replace("-", "");
        String weightPart = String.format("%02d", Math.max(0, Math.min(99, weight)));
        String prefix = TEAM_PREFIX + weightPart + "_";
        int available = 16 - prefix.length();
        return prefix + compact.substring(0, Math.min(Math.max(available, 0), compact.length()));
    }

    private String cut(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
