package fr.crewcmoi.tab.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.tab.roles.Role;
import fr.crewcmoi.tab.roles.RoleManager;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Applique le rôle d'un joueur (voir RoleManager) à l'affichage du tab et au
 * pseudo au-dessus de sa tête, via une team scoreboard par rôle : préfixe
 * coloré + tri dans le tab (les teams sont listées par ordre alphabétique de
 * leur nom, d'où le "r<weight>_" au début du nom de chaque team, qui garantit
 * que Fonda s'affiche avant Admin, avant Dev, etc.).
 *
 * Un joueur ne peut appartenir qu'à une seule team à la fois sur un même
 * scoreboard (ex: celui d'InvisibilityManager pour le rôle pvp, voir
 * fr.crewcmoi.pvp.managers.InvisibilityManager) : applyRole retire donc
 * systématiquement le joueur de sa team précédente, quelle qu'elle soit, avant
 * de l'ajouter à celle de son rôle.
 */
public class TabListManager {

    private static final String TEAM_PREFIX = "r";

    private final Main plugin;
    private final RoleManager roleManager;

    public TabListManager(Main plugin, RoleManager roleManager) {
        this.plugin = plugin;
        this.roleManager = roleManager;
    }

    /**
     * (Re)calcule le rôle du joueur et le place dans la team scoreboard
     * correspondante.
     */
    public void applyRole(Player player) {
        Role role = roleManager.getRole(player);
        Team team = getOrCreateTeam(role);
        Scoreboard scoreboard = team.getScoreboard();

        Team current = scoreboard.getEntryTeam(player.getName());
        if (current != null && current != team) {
            current.removeEntry(player.getName());
        }
        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
    }

    /**
     * Recalcule le rôle et réapplique la team de TOUS les joueurs en ligne (ex:
     * après un /role reload, ou un changement de préfixe en config.yml).
     */
    public void refreshAll() {
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            applyRole(online);
        }
    }

    private Team getOrCreateTeam(Role role) {
        Scoreboard scoreboard = plugin.getServer().getScoreboardManager().getMainScoreboard();
        String name = teamName(role);
        Team team = scoreboard.getTeam(name);
        if (team == null) {
            team = scoreboard.registerNewTeam(name);
        }
        team.setPrefix(cut(roleManager.getPrefix(role), 64));
        return team;
    }

    private String teamName(Role role) {
        return TEAM_PREFIX + role.getWeight() + "_" + role.getId();
    }

    private String cut(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
