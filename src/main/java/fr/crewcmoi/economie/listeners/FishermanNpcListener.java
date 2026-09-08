package fr.crewcmoi.economie.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.economie.gui.FishermanGuiManager;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class FishermanNpcListener implements Listener {

    private final Main plugin;
    private final FishermanGuiManager fishermanGuiManager;

    public FishermanNpcListener(Main plugin, FishermanGuiManager fishermanGuiManager) {
        this.plugin = plugin;
        this.fishermanGuiManager = fishermanGuiManager;
    }

    @EventHandler
    public void onNpcRightClick(NPCRightClickEvent event) {
        int fishermanNpcId = plugin.getConfig().getInt("fisherman.npc-id", 4);
        if (event.getNPC().getId() != fishermanNpcId) {
            return;
        }

        Player player = event.getClicker();
        fishermanGuiManager.openMenu(player);
    }
}
