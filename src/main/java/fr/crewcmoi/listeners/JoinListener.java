package fr.crewcmoi.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.managers.BountyManager;
import fr.crewcmoi.claims.managers.ClaimVisualizer;
import fr.crewcmoi.economie.managers.EconomyManager;
import fr.crewcmoi.pvp.managers.MalusEffectManager;
import fr.crewcmoi.managers.TeamManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinListener implements Listener {

    private final Main plugin;
    private final EconomyManager economyManager;
    private final TeamManager teamManager;
    private final BountyManager bountyManager;
    private final MalusEffectManager malusEffectManager;
    private final ClaimVisualizer claimVisualizer;

    public JoinListener(Main plugin, EconomyManager economyManager, TeamManager teamManager,
                         BountyManager bountyManager, MalusEffectManager malusEffectManager,
                         ClaimVisualizer claimVisualizer) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.teamManager = teamManager;
        this.bountyManager = bountyManager;
        this.malusEffectManager = malusEffectManager;
        this.claimVisualizer = claimVisualizer;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        economyManager.loadPlayer(player.getUniqueId(), player.getName());
        teamManager.loadPlayer(player.getUniqueId());
        bountyManager.refreshBountyDisplayOnJoin(player.getUniqueId(), player.getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        economyManager.unloadPlayer(player.getUniqueId());
        teamManager.unloadPlayer(player.getUniqueId());
        bountyManager.removeDisplayOnQuit(player.getUniqueId());
        malusEffectManager.clearVolatileState(player);
        claimVisualizer.stop(player);
    }
}
