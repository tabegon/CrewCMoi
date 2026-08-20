package fr.crewcmoi.listeners;

import fr.crewcmoi.gui.SellGuiManager;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Ouvre la GUI de /sell lorsqu'un joueur fait un clic droit sur le NPC
 * Citizens d'id 1, au lieu de passer par la commande /sell.
 */
public class SellNpcListener implements Listener {

    /** Id du NPC Citizens qui déclenche l'ouverture de la GUI de vente. */
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
