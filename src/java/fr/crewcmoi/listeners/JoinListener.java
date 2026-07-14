package fr.crewcmoi.listeners;

import fr.economy.Main;
import fr.economy.managers.EconomyManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinListener implements Listener {

    private final Main plugin;
    private final EconomyManager economyManager;

    public JoinListener(Main plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        economyManager.loadPlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        economyManager.unloadPlayer(event.getPlayer().getUniqueId());
    }
}
