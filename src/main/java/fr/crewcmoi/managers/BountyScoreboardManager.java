package fr.crewcmoi.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.utils.MoneyFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Affiche la prime d'un joueur sous forme de suffixe sur une scoreboard team vanilla
 * (visible directement sous son pseudo, sans dépendre d'aucun autre plugin).
 *
 * Remplace l'ancienne approche par entité TextDisplay (voir BountyDisplayManager,
 * conservée dans le projet mais plus utilisée par BountyManager). On avait initialement
 * abandonné les scoreboard teams en pensant qu'un autre plugin du serveur (LuckPerms
 * et/ou un système de nametag) reprenait la main dessus - après vérification, rien
 * d'autre sur ce serveur ne crée ni ne gère de scoreboard team, LuckPerms ne touche pas
 * du tout à la scoreboard vanilla par défaut. Le souci venait donc très probablement de
 * l'implémentation elle-même, pas d'un conflit externe.
 *
 * Chaque joueur ayant une prime active se voit attribuer sa propre team (une team =
 * une seule "entry" = son pseudo), avec un suffixe correspondant au montant. Un joueur
 * ne peut être membre que d'une seule scoreboard team sur une scoreboard donnée : comme
 * ce plugin est actuellement seul à utiliser des teams sur la scoreboard principale,
 * ceci ne rentre en conflit avec rien.
 */
public class BountyScoreboardManager {

    // Préfixe des noms de team créées par ce système (utilisé aussi pour le nettoyage
    // des teams orphelines au redémarrage). Le total (préfixe + suffixe d'UUID) reste
    // sous 16 caractères pour rester compatible avec les anciennes limites vanilla.
    private static final String TEAM_PREFIX = "ccbounty_";

    private final Main plugin;
    private Scoreboard scoreboard;

    // Team actuellement enregistrée pour chaque joueur ayant une prime active.
    private final Map<UUID, Team> teams = new ConcurrentHashMap<>();

    public BountyScoreboardManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * À appeler une seule fois, au démarrage du plugin : récupère la scoreboard
     * principale (celle utilisée par tous les joueurs par défaut) et nettoie
     * d'éventuelles teams orphelines laissées par une session précédente (ex: crash).
     */
    public void start() {
        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        for (Team team : new ArrayList<>(scoreboard.getTeams())) {
            if (team.getName().startsWith(TEAM_PREFIX)) {
                team.unregister();
            }
        }
    }

    public void stop() {
        for (Team team : teams.values()) {
            if (team.getScoreboard() != null) {
                team.unregister();
            }
        }
        teams.clear();
    }

    /**
     * Crée (ou met à jour) le suffixe de prime d'un joueur actuellement en ligne.
     * Ne fait rien si le joueur n'est pas en ligne (sera réappliqué à sa prochaine
     * connexion via BountyManager#refreshBountyDisplayOnJoin).
     */
    public void update(UUID playerUuid, double total) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null || !player.isOnline()) {
            return;
        }

        if (total <= 0) {
            remove(playerUuid);
            return;
        }

        Team team = teams.get(playerUuid);
        if (team == null) {
            String teamName = teamName(playerUuid);
            // Sécurité : si une team du même nom existe déjà sans qu'on la connaisse
            // (ex: rechargement du plugin), on la réutilise plutôt que d'échouer.
            team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }
            if (!team.hasEntry(player.getName())) {
                team.addEntry(player.getName());
            }
            teams.put(playerUuid, team);
        }

        String amountText = MoneyFormat.format(total);
        Component suffix = Component.text(amountText, NamedTextColor.GOLD)
                .append(Component.text(" \uE517", NamedTextColor.WHITE));
        team.suffix(suffix);
    }

    /**
     * Supprime le suffixe de prime d'un joueur (prime retombée à 0, ou déconnexion).
     */
    public void remove(UUID playerUuid) {
        Team team = teams.remove(playerUuid);
        if (team != null && team.getScoreboard() != null) {
            team.unregister();
        }
    }

    private String teamName(UUID uuid) {
        String compact = uuid.toString().replace("-", "");
        int available = 16 - TEAM_PREFIX.length();
        return TEAM_PREFIX + compact.substring(0, Math.min(available, compact.length()));
    }
}
