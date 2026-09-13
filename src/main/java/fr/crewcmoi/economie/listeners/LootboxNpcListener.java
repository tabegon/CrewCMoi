package fr.crewcmoi.economie.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.economie.managers.LootboxManager;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class LootboxNpcListener implements Listener {
    private final Main plugin;
    private final LootboxManager lootboxManager;

    public LootboxNpcListener(Main plugin, LootboxManager lootboxManager) {
        this.plugin = plugin;
        this.lootboxManager = lootboxManager;
    }

    @EventHandler
    public void onNpcRightClick(NPCRightClickEvent event) {
        int npcId = plugin.getConfig().getInt("npcs.lootbox", 6);
        if (event.getNPC().getId() != npcId) return;
        Player player = event.getClicker();
        lootboxManager.open(player);
    }
}
