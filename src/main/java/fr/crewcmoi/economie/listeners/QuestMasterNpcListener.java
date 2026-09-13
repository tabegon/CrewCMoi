package fr.crewcmoi.economie.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.economie.gui.QuestMasterGuiManager;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class QuestMasterNpcListener implements Listener {
    
    private final Main plugin;
    private final QuestMasterGuiManager guiManager;

    public QuestMasterNpcListener(Main plugin) {
        this.plugin = plugin;
        this.guiManager = new QuestMasterGuiManager(plugin);
    }

    @EventHandler
    public void onNpcRightClick(NPCRightClickEvent event) {
        if (event.getNPC().getId() != plugin.getConfig().getInt("npcs.quest-master", 1)) return;
        Player player = event.getClicker();
        player.sendMessage("J'aimerais que vous me trouviez mon ami dans ce monde");
        guiManager.open(player);
    }
}
