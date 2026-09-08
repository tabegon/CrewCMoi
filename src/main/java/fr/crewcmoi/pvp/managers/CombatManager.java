package fr.crewcmoi.pvp.managers;
import fr.crewcmoi.other.utils.Messages;

import fr.crewcmoi.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatManager {

    private final Main plugin;

    private final int combatDurationSeconds;

    private final Map<UUID, BukkitTask> combatTasks = new ConcurrentHashMap<>();

    private final Map<UUID, Long> combatExpiry = new ConcurrentHashMap<>();

    private final Map<UUID, UUID> firstAttacker = new ConcurrentHashMap<>();

    
    
    private final Map<UUID, UUID> engagementAggressor = new ConcurrentHashMap<>();

    
    private final Map<UUID, UUID> currentOpponent = new ConcurrentHashMap<>();

    
    private BukkitTask actionBarTask;

    public CombatManager(Main plugin) {
        this.plugin = plugin;
        this.combatDurationSeconds = plugin.getConfig().getInt("combat-log.duration-seconds", 30);
    }

    public void startActionBar() {
        if (actionBarTask != null) {
            return;
        }
        actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : combatExpiry.keySet()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    continue;
                }
                int remaining = getRemainingSeconds(player);
                if (remaining <= 0) {
                    continue;
                }
                player.sendActionBar(Component.text(Messages.get("combat-log.actionbar", java.util.Map.of("seconds", remaining)), NamedTextColor.RED));
            }
        }, 0L, 20L);
    }

    public void stopActionBar() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
    }

    private boolean isExemptFromCombat(Player player) {
        return player.getGameMode() == GameMode.CREATIVE;
    }

    public void tagCombat(Player player) {
        if (isExemptFromCombat(player)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        combatExpiry.put(uuid, System.currentTimeMillis() + (combatDurationSeconds * 1000L));

        
        if (player.isGliding()) {
            player.setGliding(false);
        }

        BukkitTask existing = combatTasks.get(uuid);
        if (existing != null) {
            existing.cancel();
        }

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            combatTasks.remove(uuid);
            combatExpiry.remove(uuid);
            firstAttacker.remove(uuid);
            engagementAggressor.remove(uuid);
            
        }, combatDurationSeconds * 20L);

        combatTasks.put(uuid, task);
    }

    public void registerAttack(Player victim, Player attacker) {
        UUID victimId = victim.getUniqueId();
        UUID attackerId = attacker.getUniqueId();
        boolean victimWasInCombat = isInCombat(victim);
        boolean attackerWasInCombat = isInCombat(attacker);

        if (!victimWasInCombat) {
            firstAttacker.put(victimId, attackerId);
            
        }

        
        if (!victimWasInCombat && !attackerWasInCombat) {
            
            engagementAggressor.put(victimId, attackerId);
            engagementAggressor.put(attackerId, attackerId);
        } else if (!victimWasInCombat) {

            UUID knownAggressor = engagementAggressor.getOrDefault(attackerId, attackerId);
            engagementAggressor.put(victimId, knownAggressor);
        } else if (!engagementAggressor.containsKey(attackerId)) {
            engagementAggressor.put(attackerId, engagementAggressor.getOrDefault(victimId, attackerId));
        }

        currentOpponent.put(victimId, attackerId);
        currentOpponent.put(attackerId, victimId);

        

        
        tagCombat(victim);
        tagCombat(attacker);
    }

    public void refreshCombat(Player player) {
        if (isInCombat(player)) {
            tagCombat(player);
        }
    }

    public boolean isInCombat(Player player) {
        Long expiry = combatExpiry.get(player.getUniqueId());
        return expiry != null && expiry > System.currentTimeMillis();
    }

    public int getRemainingSeconds(Player player) {
        Long expiry = combatExpiry.get(player.getUniqueId());
        if (expiry == null) {
            return 0;
        }
        long remainingMs = expiry - System.currentTimeMillis();
        return (int) Math.max(0, Math.ceil(remainingMs / 1000.0));
    }

    public UUID getFirstAttacker(Player player) {
        return firstAttacker.get(player.getUniqueId());
    }

    public UUID getAggressor(Player player) {
        return engagementAggressor.get(player.getUniqueId());
    }

    public UUID getOpponent(Player player) {
        return currentOpponent.get(player.getUniqueId());
    }

    public void clearCombat(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitTask task = combatTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        combatExpiry.remove(uuid);
        firstAttacker.remove(uuid);
        engagementAggressor.remove(uuid);
        currentOpponent.remove(uuid);
    }

    public void stopCombatForBoth(Player player) {
        UUID opponentId = currentOpponent.get(player.getUniqueId());
        clearCombat(player);
        if (opponentId != null) {
            Player opponent = Bukkit.getPlayer(opponentId);
            if (opponent != null) {
                clearCombat(opponent);
            }
        }
    }

    private void sendMessage(Player player, String path) {
        String message = plugin.getMessages().getString(path);
        if (message == null) {
            return;
        }
        String prefix = plugin.getMessages().getString("prefix");
        player.sendMessage((prefix + message).replace('&', '§'));
    }
}
