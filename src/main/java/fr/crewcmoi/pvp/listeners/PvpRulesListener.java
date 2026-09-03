package fr.crewcmoi.pvp.listeners;

import fr.crewcmoi.pvp.managers.CombatManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Règles de PvP additionnelles :
 * <ul>
 *     <li>Les explosions de cristaux de l'End (End Crystal) et d'ancres de résurrection
 *     (Respawn Anchor) ne blessent QUE le joueur qui a déclenché l'explosion (celui qui a
 *     frappé le cristal / rechargé l'ancre), jamais les autres joueurs pris dans le
 *     souffle.</li>
 *     <li>Les ender pearls restent utilisables en plein combat : leur utilisation
 *     rafraîchit simplement le tag de combat au lieu d'être bloquée ou ignorée.</li>
 * </ul>
 */
public class PvpRulesListener implements Listener {

    // Durée pendant laquelle on retient qui a déclenché un cristal / une ancre,
    // au cas où l'explosion mettrait un tick ou deux à se produire.
    private static final long TRIGGER_MEMORY_MS = 5000L;
    // Rayon (en blocs, au carré) dans lequel on associe une explosion d'ancre à son
    // déclencheur enregistré.
    private static final double ANCHOR_MATCH_RADIUS_SQUARED = 9.0 * 9.0;

    private final CombatManager combatManager;

    // Cristal (UUID de l'entité) -> joueur qui l'a frappé en dernier (donc celui qui va
    // déclencher son explosion).
    private final Map<UUID, TriggerRecord> crystalTriggers = new ConcurrentHashMap<>();

    // Ancres de résurrection sur le point d'exploser : localisation du bloc -> déclencheur.
    private final Map<Location, TriggerRecord> anchorTriggers = new ConcurrentHashMap<>();

    public PvpRulesListener(CombatManager combatManager) {
        this.combatManager = combatManager;
    }

    // ------------------------------------------------------------------
    // Cristaux de l'End
    // ------------------------------------------------------------------

    /**
     * Mémorise quel joueur frappe un cristal, pour savoir qui déclenche son explosion.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCrystalHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal crystal)) {
            return;
        }
        Player trigger = resolvePlayer(event.getDamager());
        if (trigger == null) {
            return;
        }
        crystalTriggers.put(crystal.getUniqueId(), new TriggerRecord(trigger.getUniqueId()));
    }

    /**
     * Applique la règle : seul le joueur ayant déclenché l'explosion du cristal peut en
     * subir les dégâts, tous les autres en sont protégés.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCrystalExplosionDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof EnderCrystal crystal)) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        purgeExpired(crystalTriggers);
        TriggerRecord record = crystalTriggers.remove(crystal.getUniqueId());
        UUID triggerId = record != null ? record.playerId : null;

        if (triggerId == null || !triggerId.equals(victim.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------
    // Ancres de résurrection (Respawn Anchor)
    // ------------------------------------------------------------------

    /**
     * Détecte le clic qui va faire exploser une ancre de résurrection (rechargée au
     * maximum, utilisée en dehors de l'Overworld) et retient qui l'a déclenchée.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onAnchorInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !(block.getBlockData() instanceof RespawnAnchor anchor)) {
            return;
        }
        if (block.getWorld().getEnvironment() == org.bukkit.World.Environment.NORMAL) {
            // Dans l'Overworld, utiliser une ancre chargée ne fait jamais exploser le bloc.
            return;
        }
        if (anchor.getCharges() != anchor.getMaximumCharges()) {
            // Seule l'utilisation d'une ancre à pleine charge la fait exploser.
            return;
        }

        purgeExpired(anchorTriggers);
        anchorTriggers.put(block.getLocation(), new TriggerRecord(event.getPlayer().getUniqueId()));
    }

    /**
     * Applique la règle : seul le joueur ayant rechargé/déclenché l'ancre peut subir les
     * dégâts de son explosion, tous les autres en sont protégés.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAnchorExplosionDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        purgeExpired(anchorTriggers);
        TriggerRecord closest = null;
        Location closestLocation = null;
        double closestDistanceSquared = ANCHOR_MATCH_RADIUS_SQUARED;

        for (Map.Entry<Location, TriggerRecord> entry : anchorTriggers.entrySet()) {
            Location anchorLocation = entry.getKey();
            if (anchorLocation.getWorld() == null || victim.getWorld() == null
                    || !anchorLocation.getWorld().equals(victim.getWorld())) {
                continue;
            }
            double distanceSquared = anchorLocation.distanceSquared(victim.getLocation());
            if (distanceSquared <= closestDistanceSquared) {
                closestDistanceSquared = distanceSquared;
                closest = entry.getValue();
                closestLocation = anchorLocation;
            }
        }

        if (closest == null) {
            // Pas d'ancre suivie à proximité : on ne touche pas à cet évènement (ex : ancre
            // détruite d'une autre façon), pour ne pas casser un autre comportement.
            return;
        }

        if (!closest.playerId.equals(victim.getUniqueId())) {
            event.setCancelled(true);
        }

        // Une ancre ne fait qu'exploser une fois : on nettoie l'entrée correspondante.
        anchorTriggers.remove(closestLocation);
    }

    // ------------------------------------------------------------------
    // Ender pearls en combat
    // ------------------------------------------------------------------

    /**
     * Autorise explicitement l'usage des ender pearls pendant un combat en cours : le tag
     * de combat est simplement rafraîchi (aucun blocage, aucune pénalité).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnderPearlThrow(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) {
            return;
        }
        if (!(pearl.getShooter() instanceof Player player)) {
            return;
        }
        combatManager.refreshCombat(player);
    }

    // ------------------------------------------------------------------
    // Utilitaires
    // ------------------------------------------------------------------

    private Player resolvePlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private <K> void purgeExpired(Map<K, TriggerRecord> map) {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<K, TriggerRecord>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue().timestamp > TRIGGER_MEMORY_MS) {
                iterator.remove();
            }
        }
    }

    private static final class TriggerRecord {
        private final UUID playerId;
        private final long timestamp;

        private TriggerRecord(UUID playerId) {
            this.playerId = playerId;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
