package fr.crewcmoi.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.managers.CombatManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Écoute les évènements liés au combat log :
 * - déclenche le tag de combat lors d'un coup ou d'un projectile reçu
 * - remet le tag à 30s lors de l'utilisation de pearls ou d'xp (fioles)
 * - bloque la téléportation et le vol en élytre pendant le combat
 * - tue le joueur s'il se déconnecte alors qu'il est en combat
 */
public class CombatListener implements Listener {

    private final Main plugin;
    private final CombatManager combatManager;

    // Commandes de téléportation à bloquer pendant le combat (en plus des events natifs)
    private static final List<String> BLOCKED_TP_COMMANDS = List.of(
            "tp", "tpa", "tpaccept", "tpask", "tphere", "teleport", "spawn", "warp", "home", "back"
    );

    public CombatListener(Main plugin, CombatManager combatManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
    }

    // --- Déclenchement du combat : coup direct ou projectile ---

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        if (victim.getGameMode() == GameMode.CREATIVE || victim.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        combatManager.registerAttack(victim, attacker);
        combatManager.tagCombat(attacker);
    }

    /**
     * Résout l'attaquant d'un dégât, qu'il s'agisse d'un coup direct ou d'un projectile
     * (flèche, trident, boule de feu, etc.) tiré par un joueur.
     */
    private Player resolveAttacker(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            if (projectile.getShooter() instanceof Player) {
                return (Player) projectile.getShooter();
            }
        }
        return null;
    }

    // --- Reset du timer à l'utilisation de perles ou d'xp pendant un combat ---

    @EventHandler(priority = EventPriority.MONITOR)
    public void onProjectileLaunch(org.bukkit.event.entity.ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player) {
            Player player = (Player) event.getEntity().getShooter();
            if (event.getEntity() instanceof org.bukkit.entity.EnderPearl && combatManager.isInCombat(player)) {
                combatManager.refreshCombat(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!combatManager.isInCombat(player)) {
            return;
        }
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.EXPERIENCE_BOTTLE) {
            combatManager.refreshCombat(player);
        }
    }

    // --- Blocage de la téléportation pendant le combat ---

    @EventHandler(priority = EventPriority.HIGH)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            // La pearl elle-même n'est pas bloquée, mais elle réinitialise le combat (voir onProjectileLaunch).
            return;
        }
        if (combatManager.isInCombat(player)) {
            event.setCancelled(true);
            sendCombatBlockedMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!combatManager.isInCombat(player)) {
            return;
        }
        String command = event.getMessage().substring(1).split(" ")[0].toLowerCase();
        if (BLOCKED_TP_COMMANDS.contains(command)) {
            event.setCancelled(true);
            sendCombatBlockedMessage(player);
        }
    }

    // --- Blocage de l'élytre pendant le combat ---

    @EventHandler(priority = EventPriority.HIGH)
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (event.isGliding() && combatManager.isInCombat(player)) {
            event.setCancelled(true);
            sendCombatBlockedMessage(player);
        }
    }

    // --- Arrêt du combat log pour les deux joueurs dès que l'un d'eux meurt ---

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (combatManager.isInCombat(victim)) {
            combatManager.stopCombatForBoth(victim);
        }
    }

    // --- Mort du joueur en cas de déconnexion pendant le combat ---

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!combatManager.isInCombat(player)) {
            return;
        }

        UUID attackerId = combatManager.getFirstAttacker(player);

        if (player.isOnline() && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setHealth(0.0);
        }

        combatManager.clearCombat(player);

        if (attackerId != null) {
            Player attacker = Bukkit.getPlayer(attackerId);
            if (attacker != null && attacker.isOnline()) {
                String message = plugin.getMessages().getString("combat-log.opponent-fled");
                if (message != null) {
                    String prefix = plugin.getMessages().getString("prefix", "");
                    attacker.sendMessage((prefix + message.replace("{player}", player.getName())).replace('&', '§'));
                }
            }
        }
    }

    private void sendCombatBlockedMessage(Player player) {
        String message = plugin.getMessages().getString("combat-log.action-blocked");
        if (message == null) {
            return;
        }
        String prefix = plugin.getMessages().getString("prefix", "");
        int seconds = combatManager.getRemainingSeconds(player);
        player.sendMessage((prefix + message.replace("{seconds}", String.valueOf(seconds))).replace('&', '§'));
    }
}
