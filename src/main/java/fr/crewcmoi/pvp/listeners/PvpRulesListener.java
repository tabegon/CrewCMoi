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

public class PvpRulesListener implements Listener {

    
    private static final long TRIGGER_MEMORY_MS = 5000L;

    private static final double ANCHOR_MATCH_RADIUS_SQUARED = 9.0 * 9.0;

    private final CombatManager combatManager;

    
    private final Map<UUID, TriggerRecord> crystalTriggers = new ConcurrentHashMap<>();

    private final Map<Location, TriggerRecord> anchorTriggers = new ConcurrentHashMap<>();

    public PvpRulesListener(CombatManager combatManager) {
        this.combatManager = combatManager;
    }

    

    

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
            
            return;
        }
        if (anchor.getCharges() != anchor.getMaximumCharges()) {
            
            return;
        }

        purgeExpired(anchorTriggers);
        anchorTriggers.put(block.getLocation(), new TriggerRecord(event.getPlayer().getUniqueId()));
    }

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

            return;
        }

        if (!closest.playerId.equals(victim.getUniqueId())) {
            event.setCancelled(true);
        }

        anchorTriggers.remove(closestLocation);
    }

    

    

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
