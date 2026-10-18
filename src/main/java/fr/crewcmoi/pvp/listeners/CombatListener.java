package fr.crewcmoi.pvp.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.managers.CombatManager;
import fr.crewcmoi.pvp.managers.MalusEffectManager;
import fr.crewcmoi.pvp.managers.InvisibilityManager;
import fr.crewcmoi.tab.managers.PlayerTeamManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;

public class CombatListener implements Listener {

    private final Main plugin;
    private final CombatManager combatManager;
    private final MalusEffectManager malusEffectManager;

    public CombatListener(Main plugin, CombatManager combatManager, MalusEffectManager malusEffectManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
        this.malusEffectManager = malusEffectManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        
        event.setDamage(malusEffectManager.applyReduction(attacker, event.getDamage()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onActualPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) return;
        if (event.getFinalDamage() <= 0.0) return;
        double healthBeforeDamage = victim.getHealth() + victim.getAbsorptionAmount();
        boolean couldBeLethal = healthBeforeDamage <= event.getFinalDamage();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!victim.isOnline() || victim.isDead() || !attacker.isOnline() || attacker.isDead()) return;
            double healthAfterDamage = victim.getHealth() + victim.getAbsorptionAmount();
            if (healthBeforeDamage - healthAfterDamage <= 0.0001) return;

            combatManager.registerAttack(victim, attacker);
            InvisibilityManager invisibilityManager = plugin.getInvisibilityManager();
            PlayerTeamManager playerTeamManager = plugin.getPlayerTeamManager();
            if (couldBeLethal && invisibilityManager != null && playerTeamManager != null
                    && invisibilityManager.isAnonymous(attacker)) {
                playerTeamManager.setTabFormattingHidden(attacker.getUniqueId(), true);
                Bukkit.getScheduler().runTask(plugin, () ->
                        playerTeamManager.setTabFormattingHidden(attacker.getUniqueId(), false));
            }
        });
    }

    private Player resolveAttacker(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND && player.isOp()) {
            return;
        }

        
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            return;
        }

        if (combatManager.isInCombat(player)) {
            event.setCancelled(true);
            sendActionBlocked(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.isGliding() && combatManager.isInCombat(player)) {
            fr.crewcmoi.pvp.managers.DuelManager duels = plugin.getDuelManager();
            fr.crewcmoi.pvp.managers.DuelSession session = duels == null ? null : duels.getSession(player.getUniqueId());
            if (session != null && session.isAllowElytra()) return;
            event.setCancelled(true);
            sendActionBlocked(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        UUID opponentId = combatManager.getOpponent(victim);
        combatManager.clearCombat(victim);
        malusEffectManager.clearVolatileState(victim);
        if (opponentId != null) {
            Player opponent = Bukkit.getPlayer(opponentId);
            if (opponent != null) {
                combatManager.clearCombat(opponent);
                malusEffectManager.clearVolatileState(opponent);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (combatManager.isInCombat(player)) {
            notifyOpponent(player);
            player.setHealth(0.0);
        }
        combatManager.clearCombat(player);
        malusEffectManager.clearVolatileState(player);
    }

    private void notifyOpponent(Player player) {
        UUID opponentId = combatManager.getOpponent(player);
        if (opponentId == null) {
            return;
        }
        Player opponent = plugin.getServer().getPlayer(opponentId);
        if (opponent == null) {
            return;
        }
        String message = plugin.getMessages().getString("combat-log.opponent-fled");
        if (message == null) {
            return;
        }
        String prefix = plugin.getMessages().getString("prefix");
        opponent.sendMessage((prefix + message)
                .replace("{player}", player.getName())
                .replace('&', '§'));
    }

    private void sendActionBlocked(Player player) {
        String message = plugin.getMessages().getString("combat-log.action-blocked");
        if (message == null) {
            return;
        }
        String prefix = plugin.getMessages().getString("prefix");
        player.sendMessage((prefix + message)
                .replace("{seconds}", String.valueOf(combatManager.getRemainingSeconds(player)))
                .replace('&', '§'));
    }
}
