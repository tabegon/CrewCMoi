package fr.crewcmoi.economie.listeners;

import fr.crewcmoi.economie.gui.SellGuiManager;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SellNpcListener implements Listener {

    private static final int SELL_NPC_ID = 0;

    private final SellGuiManager sellGuiManager;

    public SellNpcListener(SellGuiManager sellGuiManager) {
        this.sellGuiManager = sellGuiManager;
    }

    @EventHandler
    public void onNpcRightClick(NPCRightClickEvent event) {
        if (event.getNPC().getId() != SELL_NPC_ID) {
            return;
        }

        Player player = event.getClicker();
        sellGuiManager.open(player);
    }
}
