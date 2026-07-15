package fr.crewcmoi.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.managers.EconomyManager;
import fr.crewcmoi.managers.TeamManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinListener implements Listener {

    private final Main plugin;
    private final EconomyManager economyManager;
    private final TeamManager teamManager;

    public JoinListener(Main plugin, EconomyManager economyManager, TeamManager teamManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        economyManager.loadPlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        teamManager.loadPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        economyManager.unloadPlayer(event.getPlayer().getUniqueId());
        teamManager.unloadPlayer(event.getPlayer().getUniqueId());
    }
}

