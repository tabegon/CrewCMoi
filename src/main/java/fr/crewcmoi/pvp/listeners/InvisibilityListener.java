package fr.crewcmoi.pvp.listeners;

import fr.crewcmoi.pvp.managers.InvisibilityManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * Masque le pseudo (nametag) d'un joueur pour tout le monde tant qu'il a l'effet
 * d'invisibilité (potion) actif, et le réaffiche dès que l'effet se termine
 * (expiration, lait, /effect clear, mort, etc.).
 */
public class InvisibilityListener implements Listener {

    private final InvisibilityManager invisibilityManager;

    public InvisibilityListener(InvisibilityManager invisibilityManager) {
        this.invisibilityManager = invisibilityManager;
    }

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }
        if (event.getModifiedType() != PotionEffectType.INVISIBILITY) {
            return;
        }

        switch (event.getAction()) {
            case ADDED, CHANGED -> invisibilityManager.hideNameTag(player);
            case REMOVED, CLEARED -> invisibilityManager.showNameTag(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Nettoyage par sécurité : on évite qu'un joueur reste dans la team "nametag
        // caché" après sa déconnexion (par exemple s'il se déconnecte pendant l'effet).
        invisibilityManager.showNameTag(event.getPlayer());
    }
}
