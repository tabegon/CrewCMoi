package fr.crewcmoi.pvp.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.utils.MoneyFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BountyScoreboardManager implements Listener {

    private static final String TEAM_PREFIX = "ccbounty_";

    private final Main plugin;


    private final Map<UUID, Double> activeTotals = new ConcurrentHashMap<>();

    private final Map<UUID, String> activeNames = new ConcurrentHashMap<>();

    private final Map<Scoreboard, Map<UUID, Team>> teamsByScoreboard = new IdentityHashMap<>();

    public BountyScoreboardManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * À appeler une seule fois, au démarrage du plugin : nettoie d'éventuelles teams
     * orphelines laissées par une session précédente (ex: crash) sur toutes les scoreboards
     * actuellement en jeu, puis commence à écouter les connexions de joueurs.
     */
    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        for (Scoreboard scoreboard : onlineScoreboards()) {
            for (Team team : new ArrayList<>(scoreboard.getTeams())) {
                if (team.getName().startsWith(TEAM_PREFIX)) {
                    team.unregister();
                }
            }
        }
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
        activeTotals.clear();
        activeNames.clear();
    }

    /**
     * Réapplique toutes les primes actives sur la scoreboard du joueur qui vient de se
     * connecter (voir le commentaire de classe : sa scoreboard peut être une instance
     * différente de celle de tous les autres joueurs déjà en ligne).
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Scoreboard scoreboard = event.getPlayer().getScoreboard();
        for (Map.Entry<UUID, Double> entry : activeTotals.entrySet()) {
            UUID targetUuid = entry.getKey();
            applyToScoreboard(scoreboard, targetUuid, activeNames.get(targetUuid), entry.getValue());
        }
    }

    public void update(UUID targetUuid, String targetName, double total) {
        if (total <= 0) {
            remove(targetUuid);
            return;
        }

        activeTotals.put(targetUuid, total);
        activeNames.put(targetUuid, targetName);

        for (Scoreboard scoreboard : onlineScoreboards()) {
            applyToScoreboard(scoreboard, targetUuid, targetName, total);
        }
    }

    /**
     * Supprime le suffixe de prime d'un joueur de toutes les scoreboards sur lesquelles il
     * avait été appliqué (prime retombée à 0, ou déconnexion).
     */
    public void remove(UUID targetUuid) {
        activeTotals.remove(targetUuid);
        activeNames.remove(targetUuid);

        for (Map<UUID, Team> teams : teamsByScoreboard.values()) {
            Team team = teams.remove(targetUuid);
            if (team != null && team.getScoreboard() != null) {
                team.unregister();
            }
        }
    }

    private void applyToScoreboard(Scoreboard scoreboard, UUID targetUuid, String targetName, double total) {
        if (scoreboard == null || targetName == null) {
            return;
        }

        Map<UUID, Team> teams = teamsByScoreboard.computeIfAbsent(scoreboard, s -> new HashMap<>());
        Team team = teams.get(targetUuid);

        if (team == null || team.getScoreboard() == null) {
            String teamName = teamName(targetUuid);
            team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            teams.put(targetUuid, team);
        }

        if (!team.hasEntry(targetName)) {
            team.addEntry(targetName);
        }

        Component suffix = Component.text(" [" + MoneyFormat.format(total), NamedTextColor.GOLD)
                .append(Component.text(" \uE517", NamedTextColor.WHITE))
                .append(Component.text("]"));
        team.suffix(suffix);
    }

    /**
     * Toutes les scoreboards actuellement "en jeu" : la scoreboard principale, ainsi que la
     * scoreboard propre à chaque joueur en ligne si elle en utilise une différente.
     */
    private Set<Scoreboard> onlineScoreboards() {
        Set<Scoreboard> scoreboards = Collections.newSetFromMap(new IdentityHashMap<>());
        scoreboards.add(Bukkit.getScoreboardManager().getMainScoreboard());
        for (Player player : Bukkit.getOnlinePlayers()) {
            scoreboards.add(player.getScoreboard());
        }
        return scoreboards;
    }

    private String teamName(UUID uuid) {
        String compact = uuid.toString().replace("-", "");
        int available = 16 - TEAM_PREFIX.length();
        return TEAM_PREFIX + compact.substring(0, Math.min(available, compact.length()));
    }
}
