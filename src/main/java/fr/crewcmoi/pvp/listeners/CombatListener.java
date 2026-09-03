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
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;

/**
 * Gère le combat log : tag les joueurs en combat lors d'un coup PvP, bloque
 * les téléportations pendant ce délai, tue un joueur qui se déconnecte en
 * combat, et applique la réduction de dégâts liée au malus de prime serveur
 * (voir MalusEffectManager#applyReduction) sur les coups portés contre
 * d'autres joueurs.
 */
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

        // Ne réduit que les dégâts contre d'autres joueurs, uniquement si l'attaquant
        // est actuellement affecté par le malus de prime serveur.
        event.setDamage(malusEffectManager.applyReduction(attacker, event.getDamage()));

        combatManager.registerAttack(victim, attacker);

        // Pour un kill effectué sous invisibilité, le préfixe/suffixe doit rester visible
        // dans le TAB en permanence, mais ne doit pas faire partie du nom affiché dans le
        // message de mort. On masque donc temporairement uniquement le formatage de la
        // team juste avant que les dégâts mortels soient appliqués, puis on le restaure au
        // tick suivant, après la génération du message de mort.
        InvisibilityManager invisibilityManager = plugin.getInvisibilityManager();
        PlayerTeamManager playerTeamManager = plugin.getPlayerTeamManager();
        if (invisibilityManager != null
                && playerTeamManager != null
                && invisibilityManager.isAnonymous(attacker)
                && victim.getHealth() + victim.getAbsorptionAmount() <= event.getFinalDamage()) {
            playerTeamManager.setTabFormattingHidden(attacker.getUniqueId(), true);
            Bukkit.getScheduler().runTask(plugin, () ->
                    playerTeamManager.setTabFormattingHidden(attacker.getUniqueId(), false));
        }
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

        // Un joueur op qui se téléporte (ou est téléporté) via la commande vanilla /tp
        // (ou /teleport) peut toujours le faire, même s'il est actuellement en combat.
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND && player.isOp()) {
            return;
        }

        // Les ender pearls restent utilisables en combat (voir aussi PvpRulesListener,
        // qui rafraîchit le tag de combat quand une pearl est lancée).
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            return;
        }

        if (combatManager.isInCombat(player)) {
            event.setCancelled(true);
            sendActionBlocked(player);
        }
    }

    /**
     * Empêche un joueur en combat d'ouvrir son élytre (fuite aérienne). S'il était déjà
     * en train de planer au moment où il a été tagué, CombatManager#tagCombat s'occupe
     * de le faire atterrir directement.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.isGliding() && combatManager.isInCombat(player)) {
            event.setCancelled(true);
            sendActionBlocked(player);
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
        String prefix = plugin.getMessages().getString("prefix", "");
        opponent.sendMessage((prefix + message)
                .replace("{player}", player.getName())
                .replace('&', '§'));
    }

    private void sendActionBlocked(Player player) {
        String message = plugin.getMessages().getString("combat-log.action-blocked");
        if (message == null) {
            return;
        }
        String prefix = plugin.getMessages().getString("prefix", "");
        player.sendMessage((prefix + message)
                .replace("{seconds}", String.valueOf(combatManager.getRemainingSeconds(player)))
                .replace('&', '§'));
    }
}
