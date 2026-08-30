package fr.crewcmoi.pvp.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.utils.MoneyFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Affiche la prime d'un joueur via une entité TextDisplay flottante qui le suit, juste en
 * dessous de son pseudo.
 *
 * On est passé à cette méthode après avoir constaté que le suffixe d'équipe scoreboard
 * vanilla (l'ancienne approche, voir historique de BountyManager#applyBountyDisplay) était
 * bien créé et rempli côté serveur (confirmé par les logs), mais jamais rendu visuellement
 * en jeu : un autre plugin du serveur gère probablement déjà l'appartenance aux équipes
 * scoreboard (coloration de pseudo par un plugin de rangs, etc.) et reprenait la main dessus.
 * Une entité TextDisplay est un objet totalement indépendant du système d'équipes/scoreboard
 * vanilla : elle ne peut donc entrer en conflit avec aucun autre plugin de ce type.
 */
public class BountyDisplayManager {

    // Tag Bukkit posé sur chaque TextDisplay créée par ce système, utilisé pour retrouver et
    // nettoyer d'éventuelles entités orphelines restées d'une session précédente (ex: si le
    // serveur a crashé sans laisser le temps à onDisable() de tout nettoyer proprement).
    private static final String DISPLAY_TAG = "crewcmoi_bounty_display";

    private final Main plugin;

    // Entité TextDisplay actuellement affichée pour chaque joueur ayant une prime active.
    private final Map<UUID, TextDisplay> displays = new ConcurrentHashMap<>();

    // Dernier montant appliqué à chaque affichage, pour ne réécrire le texte que lorsqu'il
    // change réellement (évite de reconstruire un Component à chaque tick pour rien).
    private final Map<UUID, Double> lastAmounts = new ConcurrentHashMap<>();

    private BukkitTask followTask;

    public BountyDisplayManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Démarre la tâche répétitive qui repositionne chaque affichage à la position actuelle
     * de son joueur (le TextDisplay ne "suit" pas tout seul : on le retéléporte nous-mêmes à
     * chaque tick de la tâche). Nettoie aussi les éventuelles entités orphelines d'une
     * session précédente. À appeler une seule fois, au démarrage du plugin.
     */
    public void start() {
        // Nettoyage des entités orphelines (ex: après un crash serveur) : toute TextDisplay
        // taguée par ce système mais dont le propriétaire n'est plus en ligne (ou dont on n'a
        // pas connaissance dans "displays", ex: après un redémarrage) est supprimée.
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(TextDisplay.class)) {
                if (entity.getScoreboardTags().contains(DISPLAY_TAG)) {
                    entity.remove();
                }
            }
        }

        if (followTask != null) {
            return;
        }
        int intervalTicks = Math.max(1, plugin.getConfig().getInt("bounty.display.follow-interval-ticks", 2));
        followTask = Bukkit.getScheduler().runTaskTimer(plugin, this::followTick, 0L, intervalTicks);
    }

    public void stop() {
        if (followTask != null) {
            followTask.cancel();
            followTask = null;
        }
        for (TextDisplay display : displays.values()) {
            if (display.isValid()) {
                display.remove();
            }
        }
        displays.clear();
        lastAmounts.clear();
    }

    private void followTick() {
        for (Map.Entry<UUID, TextDisplay> entry : displays.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            TextDisplay display = entry.getValue();
            if (player == null || !player.isOnline() || !display.isValid()) {
                continue;
            }
            display.teleport(offsetLocation(player));
        }
    }

    /**
     * Position de l'affichage : juste au-dessus de la tête du joueur, en dessous de l'endroit
     * où le pseudo/nametag vanilla s'affiche habituellement (configurable, voir
     * bounty.display.height-offset).
     */
    private Location offsetLocation(Player player) {
        double heightOffset = plugin.getConfig().getDouble("bounty.display.height-offset", 0.35);
        return player.getEyeLocation().add(0, heightOffset, 0);
    }

    /**
     * Crée (ou met à jour) l'affichage de la prime d'un joueur actuellement en ligne. Ne fait
     * rien si le joueur n'est pas en ligne : l'affichage sera recréé à sa prochaine connexion
     * via refreshBountyDisplayOnJoin -> BountyManager, qui rappelle cette méthode.
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

        Double last = lastAmounts.get(playerUuid);
        TextDisplay display = displays.get(playerUuid);

        if (display == null || !display.isValid()) {
            display = player.getWorld().spawn(offsetLocation(player), TextDisplay.class, entity -> {
                entity.addScoreboardTag(DISPLAY_TAG);
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setPersistent(false);
                entity.setShadowed(true);
                entity.setSeeThrough(false);
                entity.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
                entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            });
            displays.put(playerUuid, display);
            last = null;
        }

        if (last == null || last != total) {
            display.text(Component.text(MoneyFormat.format(total), NamedTextColor.GOLD));
            lastAmounts.put(playerUuid, total);
        }
    }

    /**
     * Supprime l'affichage de prime d'un joueur (prime retombée à 0, ou déconnexion).
     */
    public void remove(UUID playerUuid) {
        TextDisplay display = displays.remove(playerUuid);
        lastAmounts.remove(playerUuid);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }
}
