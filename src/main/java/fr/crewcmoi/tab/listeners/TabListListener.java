package fr.crewcmoi.tab.listeners;

import fr.crewcmoi.tab.managers.TabListManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class TabListListener implements Listener {

    private final TabListManager tabListManager;

    public TabListListener(TabListManager tabListManager) {
        this.tabListManager = tabListManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        tabListManager.onPlayerJoin(event.getPlayer());
    }
}
