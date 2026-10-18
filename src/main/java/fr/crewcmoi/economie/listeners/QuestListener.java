package fr.crewcmoi.economie.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.economie.managers.QuestManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/** Événements liés à la quête n°1 du Quest Master. */
public final class QuestListener implements Listener {
    private final Main plugin;
    private final QuestManager questManager;

    public QuestListener(Main plugin, QuestManager questManager) {
        this.plugin = plugin;
        this.questManager = questManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!event.getEntity().getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey(plugin, "quest_1_electric_goat"),
                org.bukkit.persistence.PersistentDataType.BYTE)) return;
        event.getDrops().clear();
        questManager.handleGoatDeath(event.getEntity(), event.getEntity().getKiller());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            questManager.refreshSellBotVisibility(player);
            questManager.refreshCosmeticsNpcVisibility(player);
            questManager.refreshQuestLockedEntitiesVisibility(player);
        }, 20L);
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            questManager.refreshQuestLockedEntityVisibility(event.getEntity());
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                questManager.refreshSellBotVisibility(player);
            }
        });
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (org.bukkit.entity.Entity entity : event.getChunk().getEntities()) {
                questManager.refreshQuestLockedEntityVisibility(entity);
            }
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                questManager.refreshSellBotVisibility(player);
            }
        });
    }
}
