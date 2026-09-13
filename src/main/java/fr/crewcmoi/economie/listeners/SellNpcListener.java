package fr.crewcmoi.economie.listeners;

import fr.crewcmoi.economie.gui.SellGuiManager;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SellNpcListener implements Listener {

    private final fr.crewcmoi.Main plugin;

    private final SellGuiManager sellGuiManager;

    public SellNpcListener(fr.crewcmoi.Main plugin, SellGuiManager sellGuiManager) {
        this.plugin = plugin;
        this.sellGuiManager = sellGuiManager;
    }

    @EventHandler
    public void onNpcRightClick(NPCRightClickEvent event) {
        if (event.getNPC().getId() != plugin.getConfig().getInt("npcs.sell", 0)) {
            return;
        }

        Player player = event.getClicker();
        sellGuiManager.open(player);
    }
}
