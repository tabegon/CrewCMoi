package fr.crewcmoi.halloween.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.halloween.managers.HalloweenGuiManager;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class HalloweenNpcListener implements Listener {
    private final Main plugin;
    private final HalloweenGuiManager gui;
    public HalloweenNpcListener(Main plugin, HalloweenGuiManager gui){this.plugin=plugin;this.gui=gui;}
    @EventHandler public void onRightClick(NPCRightClickEvent event){ if(event.getNPC().getId()==plugin.getConfig().getInt("npcs.halloween", 5)) gui.openNpc(event.getClicker()); }
}
