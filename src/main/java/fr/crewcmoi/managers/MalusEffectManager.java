package fr.crewcmoi.managers;

import fr.crewcmoi.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère l'effet "malchance" (bad luck) infligé par le système de prime/malus
 * (voir BountyListener) : pendant une durée définie, le joueur affecté inflige
 * moins de dégâts lorsqu'il frappe un autre joueur. Cet effet est entièrement
 * codé en dur (pas un PotionEffect vanilla) et n'affecte que les dégâts contre
 * les joueurs, pas contre les mobs.
 */
public class MalusEffectManager {

    private final Main plugin;

    // Pourcentage de réduction des dégâts (0.30 = -30%) et durée en secondes, configurables.
    private final double reductionPercent;
    private final int durationSeconds;

    private final Map<UUID, Long> malusExpiry = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> malusTasks = new ConcurrentHashMap<>();

    public MalusEffectManager(Main plugin) {
        this.plugin = plugin;
        this.reductionPercent = clamp(plugin.getConfig().getDouble("bounty.bad-luck.damage-reduction-percent", 30.0) / 100.0);
        this.durationSeconds = plugin.getConfig().getInt("bounty.bad-luck.duration-seconds", 60);
    }

    private double clamp(double value) {
        if (value < 0) {
            return 0;
        }
        if (value > 1) {
            return 1;
        }
        return value;
    }

    /**
     * Applique (ou renouvelle) l'effet de malchance sur un joueur pour la durée configurée.
     */
    public void applyMalus(Player player) {
        if (durationSeconds <= 0 || reductionPercent <= 0) {
            return;
        }

        UUID uuid = player.getUniqueId();
        malusExpiry.put(uuid, System.currentTimeMillis() + (durationSeconds * 1000L));

        BukkitTask existing = malusTasks.get(uuid);
        if (existing != null) {
            existing.cancel();
        }

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            malusTasks.remove(uuid);
            malusExpiry.remove(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                sendMessage(p, "bounty.bad-luck-ended");
            }
        }, durationSeconds * 20L);

        malusTasks.put(uuid, task);
        sendMessage(player, "bounty.bad-luck-applied");
    }

    public boolean isAffected(Player player) {
        Long expiry = malusExpiry.get(player.getUniqueId());
        return expiry != null && expiry > System.currentTimeMillis();
    }

    /**
     * Applique la réduction de dégâts à un montant de dégâts donné si le joueur
     * qui frappe est actuellement affecté par le malus. Ne réduit que les coups
     * portés contre d'autres joueurs (vérifié par l'appelant).
     */
    public double applyReduction(Player attacker, double damage) {
        if (!isAffected(attacker)) {
            return damage;
        }
        return damage * (1.0 - reductionPercent);
    }

    public double getReductionPercent() {
        return reductionPercent;
    }

    private void sendMessage(Player player, String path) {
        String message = plugin.getMessages().getString(path);
        if (message == null) {
            return;
        }
        String prefix = plugin.getMessages().getString("prefix", "");
        player.sendMessage((prefix + message).replace('&', '§'));
    }
}
