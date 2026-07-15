package fr.crewcmoi.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.managers.BountyManager;
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
    private final BountyManager bountyManager;

    public JoinListener(Main plugin, EconomyManager economyManager, TeamManager teamManager, BountyManager bountyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.teamManager = teamManager;
        this.bountyManager = bountyManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        economyManager.loadPlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        teamManager.loadPlayer(event.getPlayer().getUniqueId());
        bountyManager.refreshBountyDisplayOnJoin(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        economyManager.unloadPlayer(event.getPlayer().getUniqueId());
        teamManager.unloadPlayer(event.getPlayer().getUniqueId());
    }
}

