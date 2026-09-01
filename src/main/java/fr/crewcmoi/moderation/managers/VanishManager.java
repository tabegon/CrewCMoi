package fr.crewcmoi.moderation.managers;

import fr.crewcmoi.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Gère le vanish (/vanish) : rend un joueur totalement invisible pour tout le
 * monde — y compris son armure (contrairement à une potion d'invisibilité, qui
 * ne cache pas l'armure) — et le fait disparaître du tab, comme s'il n'était
 * plus connecté au serveur.
 * <p>
 * Repose sur {@link Player#hidePlayer(org.bukkit.plugin.Plugin, Player)}, qui
 * empêche le client des autres joueurs de recevoir/afficher son entité (donc
 * son modèle ET son armure) et retire son entrée du tab — contrairement à
 * {@link fr.crewcmoi.tab.managers.PlayerTeamManager}, qui ne fait que masquer
 * le pseudo flottant et vider le préfixe/suffixe, mais laisse l'entité et
 * l'entrée du tab visibles (voir InvisibilityManager pour la potion classique).
 * <p>
 * État purement en mémoire (pas persisté) : un joueur vanish qui se déconnecte
 * redevient normalement visible à sa prochaine connexion.
 */
public class VanishManager {

    private final Main plugin;
    private final Set<UUID> vanished = new HashSet<>();

    public VanishManager(Main plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    /**
     * Bascule le vanish du joueur. Renvoie le nouvel état (true = maintenant
     * vanish).
     */
    public boolean toggle(Player player) {
        boolean newState = !isVanished(player.getUniqueId());
        setVanished(player, newState);
        return newState;
    }

    public void setVanished(Player player, boolean vanish) {
        UUID uuid = player.getUniqueId();

        if (vanish) {
            if (!vanished.add(uuid)) {
                return;
            }
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                if (!online.equals(player)) {
                    online.hidePlayer(plugin, player);
                }
            }
        } else {
            if (!vanished.remove(uuid)) {
                return;
            }
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                online.showPlayer(plugin, player);
            }
        }
    }

    /**
     * Force la sortie du vanish sans vérification (ex : quand le joueur quitte
     * le mode /staff, ou à sa déconnexion). Ne fait rien s'il n'était pas vanish.
     */
    public void forceUnvanish(Player player) {
        if (isVanished(player.getUniqueId())) {
            setVanished(player, false);
        }
    }

    /**
     * À appeler à la connexion d'un joueur : lui masque tous les joueurs
     * actuellement vanish (sans ça, il les verrait normalement le temps qu'un
     * de ces joueurs bascule à nouveau son vanish). Voir VanishListener.
     */
    public void applyToJoiningPlayer(Player joining) {
        for (UUID uuid : vanished) {
            Player vanishedPlayer = Bukkit.getPlayer(uuid);
            if (vanishedPlayer != null && !vanishedPlayer.equals(joining)) {
                joining.hidePlayer(plugin, vanishedPlayer);
            }
        }
    }

    /** Oublie l'état d'un joueur (ex : à sa déconnexion), sans notifier personne. */
    public void forget(UUID uuid) {
        vanished.remove(uuid);
    }
}
